// =============================================================================
// IMPORTS

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

// my imports 
import java.util.Set;
// =============================================================================



// =============================================================================
/**
 * @file   RandomNetworkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   April 2022
 *
 * A network layer that perform routing via random link selection.
 */
public class RandomNetworkLayer extends NetworkLayer {
// =============================================================================



    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================



    // =========================================================================
    /**
     * Default constructor.  Set up the random number generator.
     */
    public RandomNetworkLayer () {

        random = new Random();

    } // RandomNetworkLayer ()
    // =========================================================================

    
    /**
     * Insert an integer at a certain offset into a byte array 
     * note: do not do any woodoo with the signed bit
     */
    private void insertInt(int value, int offset, byte[] dest) {
        for (int i = offset; i < offset + Integer.BYTES; i++) {
            byte significant_byte = (byte) ((value & 0xFF000000) >>> (3 * BITS_PER_BYTE));
            value <<= BITS_PER_BYTE;

            dest[i] = significant_byte;
        }
    }

    /**
     * extracts a header segment info from
     * the first packet in the buffer
     * @param offset
     * @param buffer
     * @return length
     */
    private int getPacketInfoAt(int offset, Iterable<Byte> buffer) {

        // check the length of the first potential packet
        Iterator<Byte> i = buffer.iterator();
        byte[] header_seg = new byte[Integer.BYTES];

        // iterate until offset
        for (int idx = 0; idx < offset; idx++) i.next();

        // get the bytes
        for (int idx = 0; idx < Integer.BYTES; idx++) {
            header_seg[idx] = i.next();
        }

        // get the length as an integer
        return bytesToInt(header_seg);
    }


    // =========================================================================
    /**
     * Create a single packet containing the given data, with header that marks
     * the source and destination hosts.
     *
     * @param destination The address to which this packet is sent.
     * @param data        The data to send.
     * @return the sequence of bytes that comprises the packet.
     */
    protected byte[] createPacket (int destination, byte[] data) {

        // COMPLETE ME
        // ====================
        // The header info
        // ====================

        // length of the whole packet w/o the header
        // for size of header use bytesPerHeader
        int packet_length = data.length;

        // we have the destination given as a parameter
        // so we need the source only 
        int source = this.address; 

        // ====================
	    // Building the packet
        // ====================
        
        byte[] packet = new byte[packet_length + bytesPerHeader];

        // putting in the header info at offsets given
        insertInt(packet_length, lengthOffset, packet);        
        insertInt(source, sourceOffset, packet);
        insertInt(destination, destinationOffset, packet);

        // inserting the data itself into the packet
        for (int i = 0; i < data.length; i++) {
            packet[i + bytesPerHeader] = data[i];
        }
        
        Queue<Byte> packetQueue = new LinkedList<>();
        for (int i = 0; i < packet.length; i++) packetQueue.add(packet[i]);
        System.out.println(getPacketInfoAt(sourceOffset, packetQueue));
        return packet;
    } // createPacket ()
    // =========================================================================

    // =========================================================================
    /**
     * Randomly choose the link through which to send a packet given its
     * destination.
     * Returns null if no routes found
     *
     * @param destination The address to which this packet is being sent.
     */
    protected DataLinkLayer route (int destination) {

        // COMPLETE ME
        // use dataLinkLayers here

        // get the set of all keys (i.e. addresses) connected to the network layer
        Set<Integer> addr_set = dataLinkLayers.keySet();

        // edge case check
        if (addr_set.isEmpty()) return null;

        // pick a random number between 0 (incl) and keys.size() (excl)
        int addr_idx = random.nextInt(addr_set.size());
	
        // iterate through the keyset and stop at the idx == addr_idx
        Iterator<Integer> i = addr_set.iterator();
        // the index that we'll iterate as we go thru the iterator
        int idx = 0;

        // the address that we will choose
        int chosen_addr = 0;

        while(i.hasNext()) {
            int addr = i.next();
            
            if (idx == addr_idx) {
                chosen_addr = addr;
                break;
            }
            idx++;
        }

        // at this point we have chose our address
        // now we return the dataLinkLayer it corresponds to
        return dataLinkLayers.get(chosen_addr);
    } // route ()
    // =========================================================================



    // =========================================================================
    /**
     * Examine a buffer to see if it's data can be extracted as a packet; if so,
     * do it, and return the packet whole.
     *
     * @param buffer The receive-buffer to be examined.
     * @return the packet extracted packet if a whole one is present in the
     *         buffer; <code>null</code> otherwise.
     */
    protected byte[] extractPacket (Queue<Byte> buffer) {

        // COMPLETE ME
        // check if there are at least HEADER bytes in the buffer + 1 byte 
        if (buffer.size() <= bytesPerHeader) return null;

        int length = getPacketInfoAt(lengthOffset, buffer);

        // if not the whole packet is there yet
        if (length + bytesPerHeader > buffer.size()) return null;

        // extract the length + bytesPerHeader worth of bytes and return as an array 
        byte[] packet = new byte[length + bytesPerHeader];
        
        for (int idx = 0; idx < packet.length; idx++) {
            packet[idx] = buffer.remove();
        }


        return packet;
    } // extractPacket ()
    // =========================================================================



    // =========================================================================
    /**
     * Given a received packet, process it.  If the destination for the packet
     * is this host, then deliver its data to the client layer.  If the
     * destination is another host, route and send the packet.
     *
     * @param packet The received packet to process.
     * @see   createPacket
     */
    protected void processPacket (byte[] packet) {

        // COMPLETE ME
        // first obtain the destination address and the length
        Queue<Byte> packetQueue = new LinkedList<>();
        for (int i = 0; i < packet.length; i++) packetQueue.add(packet[i]);
        int destination = getPacketInfoAt(destinationOffset, packetQueue);
	
        // get the packet data only into a separate array
        byte[] packet_data = new byte[packet.length - bytesPerHeader];

        for (int i = bytesPerHeader; i < packet.length; i++) packet_data[i - bytesPerHeader] = packet[i];

        // check if this host is the destination 
        if (address == destination) {
            client.receive(packet_data);
            System.out.println("arrived");
            return;
        }
        
        // if destination is another host - reroute
        DataLinkLayer next_datalink = route(destination);
        next_datalink.send(packet);
    } // processPacket ()
    // =========================================================================
    


    // =========================================================================
    // INSTANCE DATA MEMBERS

    /** The random source for selecting routes. */
    private Random random;
    // =========================================================================



    // =========================================================================
    // CLASS DATA MEMBERS

    /** The offset into the header for the length. */
    public static final int     lengthOffset      = 0;

    /** The offset into the header for the source address. */
    public static final int     sourceOffset      = lengthOffset + Integer.BYTES;

    /** The offset into the header for the destination address. */
    public static final int     destinationOffset = sourceOffset + Integer.BYTES;

    /** How many total bytes per header. */
    public static final int     bytesPerHeader    = destinationOffset + Integer.BYTES;

    /** Whether to emit debugging information. */
    public static final boolean debug             = false;
   // =========================================================================

                                     
    public static final int BITS_PER_BYTE = 8;
    
// =============================================================================
} // class RandomNetworkLayer
// =============================================================================
