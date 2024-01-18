package dev.mccue.pagesize.test;

import dev.mccue.pagesize.PageSize;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PageSizeTest {
    @Test
    public void testPageSizeGTZero() {
        Assertions.assertTrue(PageSize.get() > 0);
    }

    @Test
    public void testPageSizePowerOfTwo() {
        Assertions.assertEquals(
                Math.log(PageSize.get()) / Math.log(2),
                (int) (Math.log(PageSize.get()) / Math.log(2))
        );
    }
}
