// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import scala.math.{min,max}
/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLRAMAtomicAutomata(txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("AtomicAutomata"))
  val ram  = LazyModule(new TLRAM(AddressSet(0x0, 0x3ff)))

  // Confirm that the AtomicAutomata combines read + write errors
  import TLMessages._
  val test = new RequestPattern({a: TLBundleA =>
    val doesA = a.opcode === ArithmeticData || a.opcode === LogicalData
    val doesR = a.opcode === Get || doesA
    val doesW = a.opcode === PutFullData || a.opcode === PutPartialData || doesA
    (doesR && RequestPattern.overlaps(Seq(AddressSet(0x08, ~0x08)))(a)) ||
    (doesW && RequestPattern.overlaps(Seq(AddressSet(0x10, ~0x10)))(a))
  })

  (ram.node
    := TLErrorEvaluator(test)
    := TLFragmenter(4, 256)
    := TLDelayer(0.1)
    := TLAtomicAutomata()
    := TLDelayer(0.1)
    := TLErrorEvaluator(test, testOn=true, testOff=true)
    := model.node
    := fuzz.node)

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMAtomicAutomataTest(txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMAtomicAutomata(txns)).module)
  io.finished := dut.io.finished
}
