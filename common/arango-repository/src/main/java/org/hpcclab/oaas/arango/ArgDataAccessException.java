package org.hpcclab.oaas.arango;

import com.arangodb.entity.ErrorEntity;
import org.hpcclab.oaas.model.exception.DataAccessException;
import org.hpcclab.oaas.model.exception.StdOaasException;

import java.util.Collection;

public class ArgDataAccessException extends DataAccessException {
  Collection<ErrorEntity> errors;

  public ArgDataAccessException(Throwable e) {
    super(null, e);
  }
  public ArgDataAccessException(Collection<ErrorEntity> errors) {
    super(500);
    this.errors = errors;
  }

  public Collection<ErrorEntity> getErrors() {
    return errors;
  }

  public void setErrors(Collection<ErrorEntity> errors) {
    this.errors = errors;
  }

  @Override
  public String toString() {
    var err = errors.stream()
      .map(ErrorEntity::getErrorMessage).toList();
    return "DataAccessException{" +
      "errors=" + err +
      '}';
  }
}
