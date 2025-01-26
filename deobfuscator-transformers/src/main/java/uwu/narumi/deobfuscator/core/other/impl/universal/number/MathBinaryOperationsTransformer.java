package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Simplifies math operations on two constant values.
 */
public class MathBinaryOperationsTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    FramedInstructionsStream.of(this)
        .editInstructionsStream(stream -> stream.filter(insn -> AsmMathHelper.isMathBinaryOperation(insn.getOpcode())))
        .forEach(insnContext -> {
          // Get instructions from stack that are passed
          OriginalSourceValue value1SourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 2);
          OriginalSourceValue value2SourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);
          if (!value1SourceValue.originalSource.isOneWayProduced() || !value2SourceValue.originalSource.isOneWayProduced()) {
            return;
          }

          AbstractInsnNode value1Insn = value1SourceValue.originalSource.getProducer();
          AbstractInsnNode value2Insn = value2SourceValue.originalSource.getProducer();

          if (value1Insn.isNumber() && value2Insn.isNumber()) {
            Number value1 = value1Insn.asNumber();
            Number value2 = value2Insn.asNumber();

            Number result;
            try {
              result = AsmMathHelper.mathBinaryOperation(value1, value2, insnContext.insn().getOpcode());
            } catch (ArithmeticException e) {
              // Skip division by zero
              return;
            }

            insnContext.placePops();
            insnContext.methodNode().instructions.set(insnContext.insn(), AsmHelper.numberInsn(result));

            markChange();
          }
        });
  }
}
