package it.fpagano;

import java.util.Map;
import java.util.Set;

public class Main {
	public static void main(String[] args) {
		
		Map<String, Node> m = Map.of(
				"x", new Sum(new Operand(1), new Symbol("y")),
				"y", new Abs(new Difference(new Operand(1), new Operand(3)))
				);
		
		var map = new ExpressionMap(m);

		var root = new Difference(new Operand(3),
				new Multiplication(new Sum(new Operand(5), new Symbol("x")),
						new Sum(new Symbol("x"), new Symbol("y"), new Operand(1))));

		Expression e = new Expression(root);
		Integer result = e.calc(map);
		System.out.println(result);
	}
}

record Expression(Node root) implements Node {
	public Integer calc(ExpressionMap map) {
		return switch (root) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Expression o -> o.calc(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + root);
		};
	}

	public Set<String> getReferencedSymbols() {
		return Set.of();
	}
}

record ExpressionMap(Map<String, Node> map) {
	public Expression getExpression(String symbolName) {
		return new Expression(map.get(symbolName));
	}
}

interface Node { }

@FunctionalInterface
interface Operator extends Node {
	Integer reduce(ExpressionMap map);
}

record Sum(Node... nodes) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		int n = 0;
		for (Node node : nodes) {
			n += switch (node) {
				case Operand o -> o.val();
				case Operator o -> o.reduce(map);
				case Symbol o -> o.substitute(map).calc(map);
				default -> throw new IllegalStateException("Unexpected value: " + node);
			};
		}
		return n;
	}
}

record Multiplication(Node... nodes) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		int n = 1;
		for (Node node : nodes) {
			n *= switch (node) {
				case Operand o -> o.val();
				case Operator o -> o.reduce(map);
				case Symbol o -> o.substitute(map).calc(map);
				default -> throw new IllegalStateException("Unexpected value: " + node);
			};
		}
		return n;
	}

}

record Difference(Node left, Node right) implements Operator {

	@Override
	public Integer reduce(ExpressionMap map) {
		Integer a = switch (left) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + left);
		};

		Integer b = switch (right) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + right);
		};

		return a - b;
	}
}

record Division(Node left, Node right) implements Operator {
	@Override
	public Integer reduce(ExpressionMap map) {
		Integer a = switch (left) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + left);
		};
		
		Integer b = switch (right) {
			case Operand o -> o.val();
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + right);
		};
		
		return a / b;
	}
}

record Abs(Node a) implements Operator {
	@Override
	public Integer reduce(ExpressionMap map) {
		return Math.abs(switch (a) {
			case Operand o -> o.val() ;
			case Operator o -> o.reduce(map);
			case Symbol o -> o.substitute(map).calc(map);
			default -> throw new IllegalStateException("Unexpected value: " + a);
		});
	}
}

record Operand(Integer val) implements Node { }

record Symbol(String symbol) implements Node {

	public Expression substitute(ExpressionMap maps) {
		return maps.getExpression(symbol);
	}
	
}
