import java.util.Arrays;
public class test_obj {
    public static void main(String[] args) {
        int[] slots = new int[54];

        int slot = 0;
        for (int i=0; i<14; i++) {
            System.out.println("Processing item " + i + " into slot " + slot);
            slots[slot] = 1; // item
            slots[slot+9] = 2; // pane
            slot++;
            if (slot == 9) slot = 18; // jump
        }
        for(int i=0; i<54; i++) {
            System.out.print(slots[i]);
            if((i+1)%9 == 0) System.out.println();
        }
    }
}
