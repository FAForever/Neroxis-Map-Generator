package util;

import export.SCMapExporter;
import org.junit.Test;

public class DDSHeaderTest {

    private DDSHeader instance;

    @Test
    public void TestDDSHeader() {
        DDSHeader header1 = new DDSHeader();
        DDSHeader header2 = new DDSHeader();
        DDSHeader header3 = new DDSHeader();

        header1.parseHeader(SCMapExporter.DDS_HEADER_1);
        header2.parseHeader(SCMapExporter.DDS_HEADER_2);
        header3.parseHeader(SCMapExporter.DDS_HEADER_3);
    }

}
