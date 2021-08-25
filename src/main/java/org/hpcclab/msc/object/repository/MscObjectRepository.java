package org.hpcclab.msc.object.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import org.hpcclab.msc.object.entity.MscObject;

public class MscObjectRepository implements ReactivePanacheMongoRepository<MscObject> {
}
