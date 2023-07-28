package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.Tools;
import org.prowl.distribbs.utils.compression.block.CompressedBlockInputStream;
import org.prowl.distribbs.utils.compression.block.CompressedBlockOutputStream;

import java.io.IOException;

@BBSCommand
public class OARCResponse extends Command {
    private static final Log LOG = LogFactory.getLog("OARCResponse");

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (getMode().equals(Mode.CMD)) {
            StringBuffer acceptedExtensions = new StringBuffer();
            String extensions = data[1].substring(0, data[1].length() - 1);
            LOG.debug("Client has requested OARC extensions to be enabled: " + extensions);

            // We will accept compression
            if (extensions.contains("C")) {
                acceptedExtensions.append("C");
            }

            // Now send the response once we have our accepted list.
            write(CR + "[OARC " + acceptedExtensions.toString() + "]" + CR);
            client.flush();


            // Now we can activate compression
            if (extensions.contains("C")) {


                // Compression requires is to wrap the input and output streams in a GZIP stream
                LOG.debug("Compression enabled");
                client.setOutputStream(new CompressedBlockOutputStream(client.getOutputStream(), 1024));
                client.useNewInputStream(new CompressedBlockInputStream(client.getInputStream()));

                //     client.useNewInputStream(new BufferedInputStream(client.getInputStream()));
                //     client.setOutputStream(new BufferedOutputStream(client.getOutputStream()));

                // client.useNewInputStream(new GZIPInputStream(client.getInputStream()));
                //    client.setOutputStream(new GZIPOutputStream(client.getOutputStream(), true));

                // client.useNewInputStream(new XORInputStream(client.getInputStream(),"abcd"));
                //  client.setOutputStream(new XOROutputStream(client.getOutputStream(),"abcd"));
//
//                Deflater deflater = new Deflater(Deflater.HUFFMAN_ONLY, true);
//                deflater.setStrategy(Deflater.SYNC_FLUSH);
//                 client.useNewInputStream(new DeflaterInputStream(client.getInputStream()));
//                  client.setOutputStream(new DeflaterOutputStream(client.getOutputStream(),deflater, true));

                //       client.useNewInputStream(new DeflateCompressorInputStream(client.getInputStream()));
                //         client.setOutputStream(new DeflateCompressorOutputStream(client.getOutputStream()));


//                client.getOutputStream().write("Test\r".getBytes());
//                client.getOutputStream().flush();


                //    client.useNewInputStream(new GZIPInputStream(client.getInputStream()));
                //    client.setOutputStream(new GZIPOutputStream(client.getOutputStream()));
                Tools.delay(200);

            }
            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"[OARC"};
    }
}
