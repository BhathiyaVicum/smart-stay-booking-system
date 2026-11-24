public class FamilySuite extends RoomPackage {
    public FamilySuite() {
        super("FamilySuite");
    }

    @Override
    public double getDailyRate() {
        return 18000.00;
    }

    @Override
    public double getTaxRate() {
        return 0.15;
    }

    @Override
    public double getExtraGuestCharge() {
        return 2500.00;
    }

    @Override
    public boolean getBreakfastIncluded() {
        return true;
    }

    @Override
    public int getBaseCapacity() {
        return 5;
    }
}
