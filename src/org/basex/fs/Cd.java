package org.basex.fs;

import java.io.IOException;
import org.basex.data.Nodes;
import org.basex.io.PrintOutput;

/**
 * Performs a cd command.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Hannes Schwarz - Hannes.Schwarz@gmail.com
 */
public final class Cd extends FSCmd {
  /** Specified path. */
  private String path;

  @Override
  public void args(final String args) throws FSException {
    // check default options and get path
    path = defaultOpts(args).getPath();
  }

  @Override
  public void exec(final PrintOutput out) throws IOException {
    // if there is path expression go to work
    curPre = goTo(path != null ? path : "");
    context.current(new Nodes(curPre, context.data()));
  }
}
