public class ReportStatus {

    public static final String RECEIVED = "RECEIVED";     // 접수
    public static final String IN_REVIEW = "IN_REVIEW";   // 확인 중
    public static final String COMPLETED = "COMPLETED";   // 반영 완료

    private ReportStatus() {
    }

    public static String labelOf(String status) {
        if (status == null) {
            return "접수";
        }
        switch (status) {
            case RECEIVED:
                return "접수";
            case IN_REVIEW:
                return "확인 중";
            case COMPLETED:
                return "반영 완료";
            default:
                return "접수";
        }
    }
}
