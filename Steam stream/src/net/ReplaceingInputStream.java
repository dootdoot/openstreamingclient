package net;

import java.io.*;
import java.util.*;

class ReplacingInputStream {

    LinkedList<Integer> inQueue = new LinkedList<Integer>();
    LinkedList<Integer> outQueue = new LinkedList<Integer>();
    final byte[] search, replacement;
    byte[] array;
    
    protected ReplacingInputStream(byte[] array, byte[] search, byte[] replacement) {
    	this.array = array;
        this.search = search;
        this.replacement = replacement;
    }

    private boolean isMatchFound() {
        Iterator<Integer> inIter = inQueue.iterator();
        for (int i = 0; i < search.length; i++)
            if (!inIter.hasNext() || search[i] != inIter.next())
                return false;
        return true;
    }
    
    int i = 0;
    private void readAhead() throws IOException {
        // Work up some look-ahead.
        while (inQueue.size() < search.length) {
            int next = array[i];
            inQueue.offer(next);
            if (next == -1)
                break;
        }
    }


    public int read() throws IOException {

        // Next byte already determined.
        if (outQueue.isEmpty()) {

            readAhead();

            if (isMatchFound()) {
                for (int i = 0; i < search.length; i++)
                    inQueue.remove();

                for (byte b : replacement)
                    outQueue.offer((int) b);
            } else
                outQueue.add(inQueue.remove());
        }

        return outQueue.remove();
    }
    
    public Byte[] compute(){
    	ArrayList<Byte> bal = new ArrayList<Byte>();
    	for(byte b : array){
    		try{
    			bal.add((byte) read());
    		}
    		catch(IOException e){
    			return (Byte[])bal.toArray();
    		}
    	}
    	return null;
    }

    // TODO: Override the other read methods.
}