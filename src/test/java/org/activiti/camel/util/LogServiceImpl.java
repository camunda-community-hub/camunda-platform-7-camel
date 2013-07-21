package org.activiti.camel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServiceImpl implements LogService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void debug(Object msg) {
    log.debug("LogService: {}", msg.toString());
  }

  @Override
  public void info(Object msg) {
    log.debug("LogService: {}", msg.toString());
  }

}
