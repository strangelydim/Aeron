package uk.co.real_logic.aeron.tools.perf_tools;

/**
 * Created by philip on 3/30/15.
 */
public class ThroughputencySubscriberRunner
{
    private ThroughputencySubscriberImpl impl = null;

    public ThroughputencySubscriberRunner(String[] args)
    {
        try
        {
            Class clazz = Class.forName(args[0]);
            impl = (ThroughputencySubscriberImpl) clazz.newInstance();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        impl.prepare();
        impl.run();
    }

    public static void main(String[] args)
    {
        new ThroughputencySubscriberRunner(args);
    }
}
