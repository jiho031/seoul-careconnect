public class ReportType {

    public static final String MISSING = "MISSING";           // 누락 정책
    public static final String INFO_ERROR = "INFO_ERROR";     // 정보 오류
    public static final String DEADLINE_ERROR = "DEADLINE_ERROR"; // 마감일 오류
    public static final String LINK_ERROR = "LINK_ERROR";     // 링크 오류

    private ReportType() {
    }

    public static String labelOf(String reportType) {
        if (reportType == null) {
            return "미분류";
        }
        switch (reportType) {
            case MISSING:
                return "누락 정책";
            case INFO_ERROR:
                return "정보 오류";
            case DEADLINE_ERROR:
                return "마감일 오류";
            case LINK_ERROR:
                return "링크 오류";
            default:
                return "미분류";
        }
    }
}
