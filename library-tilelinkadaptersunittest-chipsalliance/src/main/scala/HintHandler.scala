package freechips.rocketchip.tilelink

import chisel3._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.devices.tilelink.TLROM

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

//TODO ensure handler will pass through hints to clients that can handle them themselves

class TLRAMHintHandler(txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("HintHandler"))
  val ram1  = LazyModule(new TLRAM(AddressSet(0x0,   0x3ff)))
  val ram2  = LazyModule(new TLRAM(AddressSet(0x400, 0x3ff)))
  val rom   = LazyModule(new TLROM(0x800, 0x400, Seq.fill(128) { 0 }))
  val xbar  = LazyModule(new TLXbar)

  (ram1.node
    := TLDelayer(0.1)
    := TLHintHandler() // should have no state (not multi-beat)
    := TLDelayer(0.1)
    := TLHintHandler() // should have no logic
    := TLDelayer(0.1)
    := TLFragmenter(4, 64)
    := xbar.node)
  (ram2.node
    := TLFragmenter(4, 64) // should cause HintHandler to use multi-beat Put
    := TLDelayer(0.1)
    := xbar.node)
  (rom.node
    := TLFragmenter(4, 64) // should cause HintHandler to use multi-beat Get
    := xbar.node)
  (xbar.node
    := TLDelayer(0.1)
    := TLHintHandler() // multi-beat with Get, PutPartial, and passthrough
    := TLDelayer(0.1)
    := model.node
    := fuzz.node)

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMHintHandlerTest(txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMHintHandler(txns)).module)
  io <> dut.io
}
