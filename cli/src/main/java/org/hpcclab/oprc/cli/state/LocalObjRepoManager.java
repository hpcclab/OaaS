package org.hpcclab.oprc.cli.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.repository.MapEntityRepository.MapObjectRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.ObjectRepository;

import java.io.*;
import java.nio.file.Path;

/**
 * @author Pawissanutt
 */
public class LocalObjRepoManager extends ObjectRepoManager {

  final ClassControllerRegistry registry;
  final Path baseDir;
  ObjectMapper objectMapper = new ObjectMapper();

  public LocalObjRepoManager(ClassControllerRegistry registry, Path baseDir) {
    this.registry = registry;
    this.baseDir = baseDir;
  }

  @Override
  public ObjectRepository createRepo(OClass cls) {
    if (cls == null) return null;
    MutableMap<String, GOObject> objectMap = loadObj(cls.getKey());
    return new MapObjectRepository(objectMap);
  }

  @Override
  protected OClass load(String clsKey) {
    return registry.getClassController(clsKey).getCls();
  }

  MutableMap<String, GOObject> loadObj(String clsKey) {
    File file = fileOf(clsKey);
    MutableMap<String, GOObject> map = Maps.mutable.empty();
    if (!file.exists())
      return map;
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        GOObject obj = objectMapper.readValue(line, GOObject.class);
        map.put(obj.getKey(), obj);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return map;
  }

  File fileOf(String cls) {
    return baseDir.resolve(cls + ".ndjson").toFile();
  }

  void persist(String clsKey) throws IOException {
    MapObjectRepository repo = (MapObjectRepository) getOrCreate(clsKey);
    File file = fileOf(clsKey);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
      for (var obj : repo.getMap().values()) {
        String jsonString = objectMapper.writeValueAsString(obj);
        writer.write(jsonString);
        writer.newLine();
      }
    }
  }
}
