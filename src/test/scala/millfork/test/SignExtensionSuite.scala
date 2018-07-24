package millfork.test

import millfork.Cpu
import millfork.test.emu.EmuCrossPlatformBenchmarkRun
import org.scalatest.{FunSuite, Matchers}

/**
  * @author Karol Stasiak
  */
class SignExtensionSuite extends FunSuite with Matchers {

  test("Sbyte to Word") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80, Cpu.Intel8080)("""
        | word output @$c000
        | void main () {
        |   sbyte b
        |   b = -1
        |   output = b
        | }
      """.stripMargin){m =>
      m.readWord(0xc000) should equal(0xffff)
    }
  }
  test("Sbyte to Word 2") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80, Cpu.Intel8080)("""
        | word output @$c000
        | void main () {
        |   output = b()
        | }
        | sbyte b() {
        |   return -1
        | }
      """.stripMargin){m => m.readWord(0xc000) should equal(0xffff)}
  }
  test("Sbyte to Long") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80, Cpu.Intel8080)("""
        | long output @$c000
        | void main () {
        |   output = 421
        |   output += b()
        | }
        | sbyte b() {
        |   return -1
        | }
      """.stripMargin){m => m.readLong(0xc000) should equal(420)}
  }

  test("Optimize pointless sign extension") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80, Cpu.Intel8080)("""
        | array output [10] @$c000
        | word w
        | void main () {
        |   byte i
        |   sbyte b
        |   w = 435
        |   b = five()
        |   b &= $7f
        |   for i,0,paralleluntil,output.length {
        |     output[i] = i
        |   }
        |   w += b
        |   output[0] = w.lo
        |   output[1] = w.hi
        | }
        | sbyte five() {
        |   return 5
        | }
      """.stripMargin){m =>
      m.readWord(0xc000) should equal(440)
    }
  }
}
