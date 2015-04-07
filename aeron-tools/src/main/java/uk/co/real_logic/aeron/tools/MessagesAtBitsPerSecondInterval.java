package uk.co.real_logic.aeron.tools;

import uk.co.real_logic.aeron.tools.RateController.IntervalInternal;

public class MessagesAtBitsPerSecondInterval extends RateControllerInterval
{
    /* The rate we _want_ to achieve, if possible.  Might not be able
     * to hit it exactly due to receiver pacing, etc.  But it's what
     * we're aiming for. */
    private final long goalBitsPerSecond;
    /* Number of messages to send; for this interval type, this is a
     * hard number, not just a goal.  We _have_ to send this many
     * messages, no matter how long it takes or how slowly we end up
     * sending them. */
    private final long messages;

    public MessagesAtBitsPerSecondInterval(long messages, long bitsPerSecond)
    {
        this.goalBitsPerSecond = bitsPerSecond;
        this.messages = messages;
    }

    public long bitsPerSecond()
    {
        return goalBitsPerSecond;
    }

    public long messages()
    {
        return messages;
    }

    @Override
    IntervalInternal makeInternal(RateController rateController) throws Exception
    {
        return rateController.new MessagesAtBitsPerSecondInternal(rateController, messages, goalBitsPerSecond);
    }
}