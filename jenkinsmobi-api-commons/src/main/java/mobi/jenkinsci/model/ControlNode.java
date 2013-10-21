package mobi.jenkinsci.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

abstract public class ControlNode extends AbstractNode {

  @Override
  public InputStream getDownloadedObjectData() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public String getDownloadedObjectType() {
    return "";
  }

  @Override
  public void toStream(OutputStream out) {
  }
}
