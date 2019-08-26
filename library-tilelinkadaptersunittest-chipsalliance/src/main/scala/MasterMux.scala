package freechips.rocketchip.devices.tilelink

import Chisel._
import chipsalliance.rocketchip.config.{Field, Parameters}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.LFSR64
/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLMasterMuxTester(txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz1 = LazyModule(new TLFuzzer(txns))
  val fuzz2 = LazyModule(new TLFuzzer(txns))
  val model1 = LazyModule(new TLRAMModel("MasterMux1"))
  val model2 = LazyModule(new TLRAMModel("MasterMux2"))
  val mux = LazyModule(new MasterMux(uFn = _.head))
  val ram = LazyModule(new TLRAM(AddressSet(0, 0x3ff), beatBytes = 4))
  mux.node := TLFilter(TLFilter.mSelectIntersect(AddressSet( 0, ~16))) := model1.node := fuzz1.node
  mux.node := TLFilter(TLFilter.mSelectIntersect(AddressSet(16, ~16))) := model2.node := fuzz2.node
  ram.node := TLFragmenter(4, 16) := mux.node
  // how to test probe + release?

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz1.module.io.finished && fuzz2.module.io.finished
    mux.module.io.bypass := LFSR64(Bool(true))(0)
  }
}

class TLMasterMuxTest(txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLMasterMuxTester(txns)).module)
  io <> dut.io
}
