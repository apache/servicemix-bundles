package com.wordnik.swagger.core

import collection.mutable.ListBuffer
import org.slf4j.{ LoggerFactory, Logger }

object SwaggerContext {
  private val LOGGER = LoggerFactory.getLogger("com.wordnik.swagger.core.SwaggerContext")

  var suffixResponseFormat = true

  private val classLoaders = ListBuffer.empty[ClassLoader]
  registerClassLoader(this.getClass.getClassLoader)
  registerClassLoader(Thread.currentThread().getContextClassLoader())

  def registerClassLoader(cl: ClassLoader) = { 
    this.classLoaders += cl
    LOGGER.debug("======>register classloader " + cl);
  }

  def loadClass(name: String) = {
    var cls: Class[_] = null
    val itr = classLoaders.reverse.iterator
    while (cls == null && itr.hasNext) {
      try {
        cls = Class.forName(name.trim, true, itr.next)
      } catch {
        case e: ClassNotFoundException => {
          LOGGER.debug("Class %s not found in classLoader".format(name))
        }
      }
    }
    //use TCCL as fallback
    registerClassLoader(Thread.currentThread().getContextClassLoader())
    cls = Class.forName(name.trim, true, Thread.currentThread().getContextClassLoader())
    if (cls == null)
      throw new ClassNotFoundException("class " + name + " not found")
    cls
  }
}
