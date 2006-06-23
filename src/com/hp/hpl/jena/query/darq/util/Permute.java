// Permute.java -- A class generating all permutations
// source: http://www.cs.fit.edu/~ryan/java/programs/combinations/Permute.java
// modified on 24/04/2006



package com.hp.hpl.jena.query.darq.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Permute<E> implements Iterator {

   private final int size;
   private final List<E> elements;  // copy of original 0 .. size-1
   private  List<E> ar;           // array for output,  0 .. size-1
   private final int [] permutation;  // perm of nums 1..size, perm[0]=0

   private boolean next = true;

   // int[], double[] array won't work :-(
   public Permute (List<E> e) {
      size = e.size();
      elements = new ArrayList<E>(e);
      ar = new ArrayList<E>(e);
      permutation = new int [size+1];
      for (int i=0; i<size+1; i++) {
     permutation [i]=i;
      }
   }
      
   @SuppressWarnings("unchecked")
   private void formNextPermutation () {
      for (int i=0; i<size; i++) {
     // i+1 because perm[0] always = 0
         // perm[]-1 because the numbers 1..size are being permuted
          ar= new ArrayList(ar);
          ar.set(i, elements.get(permutation[i+1]-1));
      }
   }

   public boolean hasNext() {
      return next;
   }

   public void remove() throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
   }

   private void swap (final int i, final int j) {
      final int x = permutation[i];
      permutation[i] = permutation [j];
      permutation[j] = x;
   }

   // does not throw NoSuchElement; it wraps around!
   public List<E> next() throws NoSuchElementException {

      formNextPermutation ();  // copy original elements

      int i = size-1;
      while (permutation[i]>permutation[i+1]) i--;

      if (i==0) {
     next = false;
     for (int j=0; j<size+1; j++) {
        permutation [j]=j;
     }
     return ar;
      }

      int j = size;
      
      while (permutation[i]>permutation[j]) j--;
      swap (i,j);
      int r = size;
      int s = i+1;
      while (r>s) { swap(r,s); r--; s++; }

      return ar;
   }

   public String toString () {
      final int n = Array.getLength(ar);
      final StringBuffer sb = new StringBuffer ("[");
      for (int j=0; j<n; j++) {
     sb.append (Array.get(ar,j).toString());
     if (j<n-1) sb.append (",");
      }
      sb.append("]");
      return new String (sb);
   }

   public static void main(String[] args) {
       
       List<String>  al= new ArrayList<String>();
       
       for (int i=0; i<20;i++){
           al.add(String.valueOf(i));
       }
       
       Permute<String> p = new Permute<String>(al);
       
              
       int count = 0;
       
       while (p.hasNext()) {
           List<String> perm = p.next();
           
           /*for (int i=0; i<perm.size();i++) {
               System.out.println(perm.get(i));
           }
           System.out.println("------------------");*/
           count++;
           if (count % 1000 == 1) System.out.println(count);
       }
       
       
   }
   
}
