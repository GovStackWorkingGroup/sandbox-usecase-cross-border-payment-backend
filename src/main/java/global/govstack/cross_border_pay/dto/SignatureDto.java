package global.govstack.cross_border_pay.dto;

public class SignatureDto {
    private Long id;
    private String payeeIdentifier;
    private Long amount;
    private String note;

    public SignatureDto() {
    }

    public SignatureDto(Long id, String payeeIdentifier, Long amount, String note) {
        this.id = id;
        this.payeeIdentifier = payeeIdentifier;
        this.amount = amount;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getPayeeIdentifier() {
        return payeeIdentifier;
    }

    public Long getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}
