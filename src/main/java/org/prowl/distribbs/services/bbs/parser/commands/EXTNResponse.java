package org.prowl.distribbs.services.bbs.parser.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.distribbs.annotations.BBSCommand;
import org.prowl.distribbs.services.bbs.parser.Mode;
import org.prowl.distribbs.utils.Tools;
import org.prowl.distribbs.utils.compression.deflate.DeflateOutputStream;
import org.prowl.distribbs.utils.compression.deflate.InflateInputStream;
import org.prowl.distribbs.utils.compression.deflatehuffman.DeflateHuffmanOutputStream;
import org.prowl.distribbs.utils.compression.deflatehuffman.InflateHuffmanInputStream;

import java.io.IOException;


@BBSCommand
public class EXTNResponse extends Command {
    private static final Log LOG = LogFactory.getLog("EXTNResponse");

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (getMode().equals(Mode.CMD)) {
            StringBuffer acceptedExtensions = new StringBuffer();
            String extensions = data[1].substring(0, data[1].length() - 1);
            LOG.debug("Client has requested EXTN extensions to be enabled: " + extensions);

            // We will accept compression
            boolean compressionExtensionAccepted = false;
            // Deflate+Huffman compressor
            if (extensions.contains("Z")) {
                acceptedExtensions.append("Z");
                compressionExtensionAccepted = true;
            }
            // Standard deflate
            if (extensions.contains("C") && !compressionExtensionAccepted) {
                acceptedExtensions.append("C");
                compressionExtensionAccepted = true;
            }

            // Now send the response once we have our accepted list.
            write(CR + "[EXTN " + acceptedExtensions.toString() + "]" + CR);
            client.flush();

            boolean compressionEnabled = false;
            // Deflate+Huffman compression, for systems that support it.
            if (extensions.contains("Z")) {
                LOG.info("Enabling Deflate+Huffman compression");
                client.setOutputStream(new DeflateHuffmanOutputStream(client.getOutputStream()));
                client.useNewInputStream(new InflateHuffmanInputStream(client.getInputStream()));
                compressionEnabled = true;
                Tools.delay(200);
            }


            // Deflate is good at large strings.
            if (extensions.contains("C") && !compressionEnabled) {
                // Compression requires is to wrap the input and output streams in a GZIP stream
                LOG.debug("Deflate Compression enabled:" + client.getInputStream().available());
                client.setOutputStream(new DeflateOutputStream(client.getOutputStream()));
                client.useNewInputStream(new InflateInputStream(client.getInputStream()));
                compressionEnabled = true;
                Tools.delay(200);
            }


            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"[EXTN"};
    }
}
