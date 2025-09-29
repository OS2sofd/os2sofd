package dk.digitalidentity.sofd.util;


import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.function.Supplier;

public class DateConverter {

    private static final SimpleDateFormat yyyyMMdd =  new SimpleDateFormat("yyyy-MM-dd");

    /***
     *
     * @param date
     * @return returns the date converted to LocalDate ignoring timezone and timepart. Null input returns null.
     */
    public static LocalDate toLocalDate(Date date) {
        if( date == null ) {
            return null;
        }
        return LocalDate.parse( yyyyMMdd.format(date) );
    }

    public static <E> LocalDate toLocalDate(Supplier<E> supplier) {
        try {
            Date date = (Date) supplier.get();
            return toLocalDate(date);
        } catch (NullPointerException npe) {
            return null;
        }
    }
}
