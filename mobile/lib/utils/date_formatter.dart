class DateFormatter {
  /// Formats a DateTime object into 'yyyy-MM-dd' string format.
  static String formatDate(DateTime dt) {
    final year = dt.year;
    final month = dt.month.toString().padLeft(2, '0');
    final day = dt.day.toString().padLeft(2, '0');
    return "$year-$month-$day";
  }

  /// Parses a 'yyyy-MM-dd' string into a DateTime object.
  static DateTime parseDate(String dateStr) {
    try {
      return DateTime.parse(dateStr);
    } catch (_) {
      return DateTime.now();
    }
  }
}
