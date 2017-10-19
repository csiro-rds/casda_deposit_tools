package au.csiro.casda.datadeposit;

/**
 * A limited use deposit state suitable only for setting a state type value in an object but not progressing it.
 */
public class DepositStateImpl extends DepositState
{

    /**
     * Create a new DepositStateImpl instance.
     * 
     * @param type
     *            The DepositState type being set.
     * @param depositable
     *            The object the state is being used for.
     */
    public DepositStateImpl(Type type, Depositable depositable)
    {
        super(type, null, depositable);
    }

    @Override
    public void progress()
    {
        throw new IllegalEventException("progress", this);
    }

    @Override
    public boolean isCheckpointState()
    {
        return true;
    }
}