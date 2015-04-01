package uk.co.real_logic.aeron.tools.perf_tools;

import java.lang.reflect.Constructor;

public class PongRunner
{
  private PongImpl impl = null;

  public PongRunner(String[] args)
  {
    try
    {
      Class<?> cl = Class.forName(args[0]);
      Constructor<?> cons = cl.getConstructor();
      impl = (PongImpl)cons.newInstance();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    impl.prepare();
    impl.run();
  }

  public static void main(String[] args)
  {
    new PongRunner(args);
  }
}
