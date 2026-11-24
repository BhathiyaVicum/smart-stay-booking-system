public class InvoiceManager {
    public Invoice calculateInvoice(Booking booking) {
        Invoice invoice = new Invoice(booking);
        invoice.calculateBill();
        return invoice;
    }

    public void generateDetailedInvoice(Invoice invoice) {
        invoice.generateDetailedInvoice();
    }
}