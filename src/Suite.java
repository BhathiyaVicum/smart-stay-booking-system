public class Suite extends RoomPackage {
    public Suite() {
        super("Suite");
    }

    @Override
    public double getDailyRate() {
        return 14000.00;
    }

    @Override
    public double getTaxRate() {
        return 0.15;
    }

    @Override
    public double getExtraGuestCharge() {
        return 2000.00;
    }

    @Override
    public boolean getBreakfastIncluded() {
        return true;
    }

    @Override
    public int getBaseCapacity() {
        return 3;
    }
}
