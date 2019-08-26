// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import scala.math.{min,max}

/** Synthesizeable unit tests */
import freechips.rocketchip.unittest._

class TLRAMFragmenter(ramBeatBytes: Int, maxSize: Int, txns: Int)(implicit p: Parameters) extends LazyModule {
  val fuzz = LazyModule(new TLFuzzer(txns))
  val model = LazyModule(new TLRAMModel("Fragmenter"))
  val ram  = LazyModule(new TLRAM(AddressSet(0x0, 0x3ff), beatBytes = ramBeatBytes))

  (ram.node
    := TLDelayer(0.1)
    := TLBuffer(BufferParams.flow)
    := TLDelayer(0.1)
    := TLFragmenter(ramBeatBytes, maxSize, earlyAck = EarlyAck.AllPuts)
    := TLDelayer(0.1)
    := TLBuffer(BufferParams.flow)
    := TLFragmenter(ramBeatBytes, maxSize/2)
    := TLDelayer(0.1)
    := TLBuffer(BufferParams.flow)
    := model.node
    := fuzz.node)

  lazy val module = new LazyModuleImp(this) with UnitTestModule {
    io.finished := fuzz.module.io.finished
  }
}

class TLRAMFragmenterTest(ramBeatBytes: Int, maxSize: Int, txns: Int = 5000, timeout: Int = 500000)(implicit p: Parameters) extends UnitTest(timeout) {
  val dut = Module(LazyModule(new TLRAMFragmenter(ramBeatBytes,maxSize,txns)).module)
  io.finished := dut.io.finished
}
