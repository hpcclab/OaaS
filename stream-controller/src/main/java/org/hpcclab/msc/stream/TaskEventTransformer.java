package org.hpcclab.msc.stream;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hpcclab.oaas.model.TaskEvent;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TaskEventTransformer implements Transformer<String,TaskEvent, Iterable<KeyValue<String, TaskEvent>>> {

  KeyValueStore<String, TaskEvent> tsStore;
  ProcessorContext context;

  final String storeName;

  public TaskEventTransformer(String storeName) {
    this.storeName = storeName;
  }

  @Override
  public void init(ProcessorContext context) {
    this.context = context;
    tsStore = context.getStateStore(storeName);
  }

  @Override
  public List<KeyValue<String, TaskEvent>> transform(String readOnlyKey, TaskEvent value) {
    return null;
  }

  @Override
  public void close() {

  }
}
