package org.hpcclab.msc.stream;

import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;

public class DeduplicationTransformer<K, V, E> implements ValueTransformerWithKey<K, V, V> {

  private ProcessorContext context;

  /**
   * Key: ip address
   * Value: timestamp (event-time) of the corresponding event when the event ID was seen for the
   * first time
   */
  private WindowStore<E, Long> eventIdStore;

  private final String storeName;

  private final long leftDurationMs;
  private final long rightDurationMs;

  private final KeyValueMapper<K, V, E> idExtractor;

  /**
   * @param maintainDurationPerEventInMs how long to "remember" a known ip address
   *                                     during the time of which any incoming duplicates
   *                                     will be dropped, thereby de-duplicating the
   *                                     input.
   * @param idExtractor                  extracts a unique identifier from a record by which we de-duplicate input
   *                                     records; if it returns null, the record will not be considered for
   *                                     de-duping but forwarded as-is.
   */
  public DeduplicationTransformer(final long maintainDurationPerEventInMs,
                                  final KeyValueMapper<K, V, E> idExtractor,
                                  final String storeName) {
    if (maintainDurationPerEventInMs < 1) {
      throw new IllegalArgumentException("maintain duration per event must be >= 1");
    }
    leftDurationMs = maintainDurationPerEventInMs / 2;
    rightDurationMs = maintainDurationPerEventInMs - leftDurationMs;
    this.idExtractor = idExtractor;
    this.storeName = storeName;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void init(final ProcessorContext context) {
    this.context = context;
    eventIdStore = context.getStateStore(storeName);
  }

  @Override
  public V transform(final K key, final V value) {
    final E eventId = idExtractor.apply(key, value);
    if (eventId==null) {
      return value;
    } else {
      final V output;
      if (isDuplicate(eventId)) {
        output = null;
        updateTimestampOfExistingEventToPreventExpiry(eventId, context.timestamp());
      } else {
        output = value;
        rememberNewEvent(eventId, context.timestamp());
      }
      return output;
    }
  }

  private boolean isDuplicate(final E eventId) {
    final long eventTime = context.timestamp();
    final WindowStoreIterator<Long> timeIterator = eventIdStore.fetch(
      eventId,
      eventTime - leftDurationMs,
      eventTime + rightDurationMs);
    final boolean isDuplicate = timeIterator.hasNext();
    timeIterator.close();
    return isDuplicate;
  }

  private void updateTimestampOfExistingEventToPreventExpiry(final E eventId, final long newTimestamp) {
    eventIdStore.put(eventId, newTimestamp, newTimestamp);
  }

  private void rememberNewEvent(final E eventId, final long timestamp) {
    eventIdStore.put(eventId, timestamp, timestamp);
  }

  @Override
  public void close() {
    // Note: The store should NOT be closed manually here via `eventIdStore.close()`!
    // The Kafka Streams API will automatically close stores when necessary.
  }
}
