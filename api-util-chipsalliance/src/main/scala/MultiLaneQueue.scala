// See LICENSE.SiFive for license details.

package freechips.rocketchip.util

import chisel3._
import chisel3.util._

class MultiLaneQueue[T <: Data](gen: T, val lanes: Int, val rows: Int, val flow: Boolean = false, storage: LanePositionedQueue = FloppedLanePositionedQueue) extends Module {
  val laneBits1 = log2Ceil(lanes+1) // [0, lanes]

  val io = IO(new Bundle {
    val enq_ready = Output(Bool())
    val enq_valid = Input(UInt(laneBits1.W))
    val enq_bits  = Input(Vec(lanes, gen))
    // NOTE: deq_valid depends on deq_ready
    val deq_ready = Input(UInt(laneBits1.W))
    val deq_valid = Output(Bool())
    val deq_bits  = Output(Vec(lanes, gen))
  })

  val queue = Module(storage(gen, lanes, rows, flow))

  io.enq_ready := io.enq_valid <= queue.io.enq.ready
  queue.io.enq.valid := Mux(io.enq_ready, io.enq_valid, 0.U)
  queue.io.enq.bits := RotateVector.left(io.enq_bits, queue.io.enq_0_lane)

  io.deq_valid := io.deq_ready <= queue.io.deq.valid
  queue.io.deq.ready := Mux(io.deq_valid, io.deq_ready, 0.U)
  io.deq_bits := RotateVector.right(queue.io.deq.bits, queue.io.deq_0_lane)
}

object RotateVector {
  def left[T <: Data](input: Seq[T], shift: UInt): Vec[T] = {
    val bools = shift.asBools.toVector
    def helper(bit: Int, offset: Int, x: Vector[T]): Vector[T] = {
      if (offset >= input.size) {
        x
      } else {
        helper(bit+1, offset+offset, Vector.tabulate(x.size) { i =>
          Mux(bools(bit), x((i+x.size-offset) % x.size), x(i))
        })
      }
    }
    VecInit(helper(0, 1, input.toVector))
  }
  def right[T <: Data](input: Seq[T], shift: UInt): Vec[T] = {
    val bools = shift.asBools.toVector
    def helper(bit: Int, offset: Int, x: Vector[T]): Vector[T] = {
      if (offset >= input.size) {
        x
      } else {
        helper(bit+1, offset+offset, Vector.tabulate(x.size) { i =>
          Mux(bools(bit), x((i+offset) % x.size), x(i))
        })
      }
    }
    VecInit(helper(0, 1, input.toVector))
  }
}
