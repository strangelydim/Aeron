package uk.co.real_logic.aeron.tools;


import org.apache.commons.cli.ParseException;

public class PublisherTool
{
    public static void main(final String[] args)
    {
        final PubSubOptions opts = new PubSubOptions();
        try
        {
            if (opts.parseArgs(args) != 0)
            {
                opts.printHelp(PublisherToolImpl.APP_USAGE);
                System.exit(0);
            }
        }

        catch (final ParseException ex)
        {
            ex.printStackTrace();
            opts.printHelp(PublisherToolImpl.APP_USAGE);
            System.exit(-1);
        }
        @SuppressWarnings("unused")
        final PublisherToolImpl app = new PublisherToolImpl(opts);
    }
}
