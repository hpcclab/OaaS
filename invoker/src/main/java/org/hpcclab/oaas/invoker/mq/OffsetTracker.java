package org.hpcclab.oaas.invoker.mq;

import java.util.Arrays;
import java.util.BitSet;

public class OffsetTracker {
  public static final long RESET_THRESHOLD = 1_000_000;
  private BitSet statusBits;
  private long initOffset;
  private long committed;

  public OffsetTracker(long initOffset) {
    this.initOffset = initOffset;
    this.committed = Math.max(initOffset, 0);
    this.statusBits = new BitSet();
  }

  long offsetToCommit() {
    return initOffset + statusBits.nextClearBit((int) (committed - initOffset));
  }

  public void setCommitted(long committed) {
    this.committed = committed;
  }

  public long getCommitted() {
    return committed;
  }

  void recordDone(long offset) {
    int relative = (int) (offset - initOffset);
    if (relative < 0)
      return;
    statusBits.set(relative);
    resetIfNeeded();
  }

  private void resetIfNeeded() {
    if (committed - initOffset < RESET_THRESHOLD)
      return;

    var statusLongArray = statusBits.toLongArray();
    long relativeOffset = committed - initOffset;
    int wordOfCommitted = (int) (relativeOffset / 64);

    var newCommittedOffsetsArr = Arrays.copyOfRange(
      statusLongArray,
      wordOfCommitted,
      statusLongArray.length
    );

    this.statusBits = BitSet.valueOf(newCommittedOffsetsArr);
    this.initOffset = committed;
  }
}
