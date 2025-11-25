package org.zefer.pixelmeister.commands;

import org.zefer.pixelmeister.DataLayerView;

public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	public PropertyTester() {
//		System.out.println("instantiated");
	}
	
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

		if (property.equals("UNDO")) {
//			System.out.print("eval UNDO... ");
			boolean res = DataLayerView.hasUndo();
//			System.out.println(res?"true":"false");
			return res;
		}

		if (property.equals("REDO")) {
//			System.out.print("eval REDO... ");
			boolean res = DataLayerView.hasRedo();
//			System.out.println(res?"true":"false");
			return res;
		}

		if (property.equals("FIND")) {
//			System.out.print("eval FIND... ");
			boolean res = DataLayerView.getActiveViewIndex() >= 0;
//			System.out.println(res?"true":"false");
			return res;
		}

		if (property.equals("RUN")) {
//			System.out.print("eval RUN... ");
			boolean res = DataLayerView.stop; 
//			System.out.println(res?"true":"false");
			return res;
		}

		if (property.equals("TERMINATE")) {
//			System.out.print("eval TERMINATE... ");
			boolean res = !DataLayerView.stop; 
//			System.out.println(res?"true":"false");
			return res;
		}

		return false;
	}
}
