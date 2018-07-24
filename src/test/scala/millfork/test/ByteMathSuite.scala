package millfork.test

import millfork.Cpu
import millfork.test.emu.{EmuBenchmarkRun, EmuCrossPlatformBenchmarkRun, EmuUltraBenchmarkRun}
import org.scalatest.{FunSuite, Matchers}

/**
  * @author Karol Stasiak
  */
class ByteMathSuite extends FunSuite with Matchers {

  test("Complex expression") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | byte output @$c000
        | void main () {
        |  output = (one() + one()) | (((one()<<2)-1) ^ one())
        | }
        | byte one() {
        |   return 1
        | }
      """.stripMargin)(_.readByte(0xc000) should equal(2))
  }

  test("Byte addition") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | byte output @$c000
        | byte a
        | void main () {
        |  a = 1
        |  output = a + a
        | }
      """.stripMargin)(_.readByte(0xc000) should equal(2))
  }

  test("Byte addition 2") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | byte output @$c000
        | byte a
        | void main () {
        |  a = 1
        |  output = a + 65
        | }
      """.stripMargin)(_.readByte(0xc000) should equal(66))
  }

  test("In-place byte addition") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | array output[3] @$c000
        | byte a
        | void main () {
        |  a = 1
        |  output[1] = 5
        |  output[a] += 1
        |  output[a] += 36
        | }
      """.stripMargin)(_.readByte(0xc001) should equal(42))
  }

  test("LHS evaluation during in-place byte addition") {
    EmuBenchmarkRun(
      """
        | array output[1] @$c000
        | byte call_count @$c001
        | void main () {
        |   output[0] = 1
        |   output[identity(0)] += identity(1)
        | }
        | noinline byte identity(byte a) {
        |   call_count += 1
        |   return a
        | }
      """.stripMargin){m =>
      m.readByte(0xc000) should equal(2)
      // TODO: currently the compiler emits separate evaluations of the left hand side for reading and writing
      // m.readByte(0xc001) should equal(2)
    }
  }

  test("Parameter order") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | byte output @$c000
        | array arr[6]
        | void main () {
        |  output = 42
        | }
        | byte test1(byte a) @$6000 {
        |   return 5 + a
        | }
        | byte test2(byte a) @$6100 {
        |   return 5 | a
        | }
        | byte test3(byte a) @$6200 {
        |   return a + arr[a]
        | }
      """.stripMargin)(_.readByte(0xc000) should equal(42))
  }

  test("In-place byte addition 2") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      """
        | array output[3] @$c000
        | void main () {
        |  byte x
        |  byte y
        |  byte tmpx
        |  byte tmpy
        |  tmpx = one()
        |  tmpy = one()
        |  x = tmpx
        |  y = tmpy
        |  output[y] = 36
        |  output[x] += 1
        | }
        | byte one() { return 1 }
      """.stripMargin)(_.readByte(0xc001) should equal(37))
  }

  test("In-place byte multiplication") {
    multiplyCase1(0, 0)
    multiplyCase1(0, 1)
    multiplyCase1(0, 2)
    multiplyCase1(0, 5)
    multiplyCase1(1, 0)
    multiplyCase1(5, 0)
    multiplyCase1(7, 0)
    multiplyCase1(2, 5)
    multiplyCase1(7, 2)
    multiplyCase1(100, 2)
    multiplyCase1(54, 4)
    multiplyCase1(2, 100)
    multiplyCase1(4, 54)
  }

  private def multiplyCase1(x: Int, y: Int): Unit = {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      s"""
         | byte output @$$c000
         | void main () {
         |  output = $x
         |  output *= $y
         | }
          """.
        stripMargin)(_.readByte(0xc000) should equal(x * y))
  }

  test("Byte multiplication") {
    multiplyCase2(0, 0)
    multiplyCase2(0, 1)
    multiplyCase2(0, 2)
    multiplyCase2(0, 5)
    multiplyCase2(1, 0)
    multiplyCase2(5, 0)
    multiplyCase2(7, 0)
    multiplyCase2(2, 5)
    multiplyCase2(7, 2)
    multiplyCase2(100, 2)
    multiplyCase2(54, 4)
    multiplyCase2(2, 100)
    multiplyCase2(4, 54)
  }

  private def multiplyCase2(x: Int, y: Int): Unit = {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      s"""
         | byte output @$$c000
         | void main () {
         |  byte a
         |  a = $x
         |  output = a * $y
         | }
          """.
        stripMargin)(_.readByte(0xc000) should equal(x * y))
  }

  test("Byte multiplication 2") {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80, Cpu.Intel8080)(
      """
        | import zp_reg
        | byte output1 @$c001
        | byte output2 @$c002
        | void main () {
        |   calc1()
        |   crash_if_bad()
        |   calc2()
        |   crash_if_bad()
        |   calc3()
        |   crash_if_bad()
        | }
        |
        | byte three() { return 3 }
        | byte four() { return 4 }
        | noinline byte five() { return 5 }
        |
        | noinline void calc1() {
        |   output1 = five() * four()
        |   output2 = 3 * three() * three()
        | }
        |
        | noinline void calc2() {
        |   output2 = 3 * three() * three()
        |   output1 = five() * four()
        | }
        |
        | noinline void calc3() {
        |   output2 = 3 * three() * three()
        |   output1 = four() * five()
        | }
        |
        | noinline void crash_if_bad() {
        | #if ARCH_6502
        |   if output1 != 20 { asm { lda $bfff }}
        |   if output2 != 27 { asm { lda $bfff }}
        | #elseif ARCH_I80
        |   if output1 != 20 { asm { ld a,($bfff) }}
        |   if output2 != 27 { asm { ld a,($bfff) }}
        | #else
        | #error unsupported architecture
        | #endif
        | }
      """.stripMargin){m =>
      m.readByte(0xc002) should equal(27)
      m.readByte(0xc001) should equal(20)
    }
  }

  test("Byte multiplication 3") {
    multiplyCase3(0, 0)
    multiplyCase3(0, 1)
    multiplyCase3(0, 2)
    multiplyCase3(0, 5)
    multiplyCase3(1, 0)
    multiplyCase3(5, 0)
    multiplyCase3(7, 0)
    multiplyCase3(2, 5)
    multiplyCase3(7, 2)
    multiplyCase3(100, 2)
    multiplyCase3(54, 4)
    multiplyCase3(2, 100)
    multiplyCase3(4, 54)
  }

  private def multiplyCase3(x: Int, y: Int): Unit = {
    EmuCrossPlatformBenchmarkRun(Cpu.Mos, Cpu.Z80)(
      s"""
         | import zp_reg
         | byte output @$$c000
         | void main () {
         |  byte a
         |  a = f()
         |  output = a * g()
         | }
         | byte f() {return $x}
         | byte g() {return $y}
          """.
        stripMargin)(_.readByte(0xc000) should equal(x * y))
  }
}
