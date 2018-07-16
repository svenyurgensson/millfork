package millfork.assembly.mos.opt

import millfork.assembly.mos.AddrMode._
import millfork.assembly.mos.AssemblyLine
import millfork.assembly.mos.Opcode._
import millfork.assembly.{AssemblyOptimization, OptimizationContext}
import millfork.env._
import millfork.error.ErrorReporting

/**
  * @author Karol Stasiak
  */
object EmptyParameterStoreRemoval extends AssemblyOptimization[AssemblyLine] {
  override def name = "Removing pointless stores to foreign variables"

  private val storeInstructions = Set(STA, STX, STY, SAX, STZ, STA_W, STX_W, STY_W, STZ_W)
  private val storeAddrModes = Set(Absolute, ZeroPage, AbsoluteX, AbsoluteY, ZeroPageX, ZeroPageY)

  override def optimize(f: NormalFunction, code: List[AssemblyLine], optimizationContext: OptimizationContext): List[AssemblyLine] = {
    val usedFunctions = code.flatMap {
      case AssemblyLine(JSR | BSR | JMP, _, MemoryAddressConstant(th), _) => Some(th.name)
      case AssemblyLine(JSR | BSR | JMP, _, NumericConstant(addr, _), _) => Some("$" + addr.toHexString)
      case _ => None
    }.toSet
    val foreignVariables = f.environment.root.things.values.flatMap {
      case other: NormalFunction =>
        val address = other.address match {
          case Some(NumericConstant(addr, _)) => "$" + addr.toHexString
          case _ => ""
        }
        if (other.name == f.name || usedFunctions(other.name) || usedFunctions(address)) {
          Nil
        } else {
          val params = other.params match {
            case NormalParamSignature(ps) => ps.map(_.name)
            case _ => Nil
          }
          val locals = other.environment.things.values.flatMap{
            case th: MemoryVariable if th.alloc == VariableAllocationMethod.Auto => Some(th.name)
            case th: MemoryVariable if th.alloc == VariableAllocationMethod.Zeropage => Some(th.name) // TODO: ???
            case _ => None
          }
          params ++ locals
        }
      case _ => Nil
    }.toSet
    val stillReadOrStoredVariables = code.flatMap {
      case AssemblyLine(_, _, MemoryAddressConstant(th), _) => Some(th.name)
      case AssemblyLine(_, _, CompoundConstant(_, MemoryAddressConstant(th), _), _) => Some(th.name)
      case AssemblyLine(_, Immediate, SubbyteConstant(MemoryAddressConstant(th), _), _) => Some(th.name)
      case _ => None
    }.toSet
    val stillReadVariables = code.flatMap {
      case AssemblyLine(op, am, MemoryAddressConstant(th), true)
        if storeInstructions(op) && storeAddrModes(am) => Nil
      case AssemblyLine(op, am, CompoundConstant(MathOperator.Plus, MemoryAddressConstant(th), NumericConstant(_, _)), true)
        if storeInstructions(op) && storeAddrModes(am) => Nil
      case AssemblyLine(_, _, MemoryAddressConstant(th), _) => Some(th.name)
      case AssemblyLine(_, _, CompoundConstant(_, MemoryAddressConstant(th), _), _) => Some(th.name)
      case AssemblyLine(_, Immediate, SubbyteConstant(MemoryAddressConstant(th), _), _) => Some(th.name)
      case _ => None
    }.toSet

    val unusedForeignVariables = (foreignVariables & stillReadOrStoredVariables) -- stillReadVariables
    if (unusedForeignVariables.isEmpty) {
      return code
    }

    ErrorReporting.debug(s"Removing pointless store(s) to foreign variables ${unusedForeignVariables.mkString(", ")}")
    code.filterNot {
      case AssemblyLine(op, am, MemoryAddressConstant(th), _)
        if storeInstructions(op) && storeAddrModes(am) =>
        unusedForeignVariables(th.name)
      case AssemblyLine(op, am, CompoundConstant(MathOperator.Plus, MemoryAddressConstant(th), NumericConstant(_, _)), true)
        if storeInstructions(op) && storeAddrModes(am) =>
        unusedForeignVariables(th.name)
      case _ => false
    }
  }
}