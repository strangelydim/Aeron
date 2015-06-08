package uk.co.real_logic.aeron.tools;

import org.apache.commons.cli.ParseException;


public class SubscriberTool
{
    public static void main(final String[] args)
    {
        final PubSubOptions opts = new PubSubOptions();

        try
        {
            if (1 == opts.parseArgs(args))
            {
                opts.printHelp("SubscriberTool");
                System.exit(-1);
            }
        }
        catch (final ParseException e)
        {
            e.printStackTrace();
            opts.printHelp("SubscriberTool");
            System.exit(-1);
        }
        final SubscriberToolImpl subTool = new SubscriberToolImpl(opts);
    }
}
