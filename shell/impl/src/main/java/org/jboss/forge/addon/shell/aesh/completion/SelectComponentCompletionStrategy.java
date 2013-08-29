/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.shell.aesh.completion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.parser.Parser;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.shell.ui.ShellContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.util.InputComponents;

/**
 * Called when auto-completion of a {@link UISelectOne} or {@link UISelectMany} component is needed
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
enum SelectComponentCompletionStrategy implements CompletionStrategy
{
   INSTANCE;

   @SuppressWarnings("unchecked")
   @Override
   public void complete(final CompleteOperation completeOperation, final InputComponent<?, Object> input,
            final ShellContext context,
            final String typedValue, final ConverterFactory converterFactory)
   {
      SelectComponent<?, Object> selectComponent = (SelectComponent<?, Object>) input;
      Converter<Object, String> itemLabelConverter = (Converter<Object, String>) InputComponents
               .getItemLabelConverter(converterFactory, selectComponent);
      Iterable<Object> valueChoices = selectComponent.getValueChoices();
      List<String> choices = new ArrayList<String>();
      for (Object choice : valueChoices)
      {
         String convert = itemLabelConverter.convert(choice);
         if (convert != null)
         {
            choices.add(convert);
         }
      }
      // Remove already set values in many valued components
      if (selectComponent instanceof ManyValued)
      {
         Object value = InputComponents.getValueFor(selectComponent);
         if (value != null)
         {
            if (value instanceof Iterable)
            {
               Iterator<Object> it = ((Iterable<Object>) value).iterator();
               while (it.hasNext())
               {
                  Object next = it.next();
                  String convert = itemLabelConverter.convert(next);
                  choices.remove(convert);
               }
            }
            else
            {
               String convert = itemLabelConverter.convert(value);
               choices.remove(convert);
            }
         }
      }
      if (choices.size() > 1)
      {
         String startsWith = Parser.findStartsWith(choices);
         if (startsWith.length() > typedValue.length())
         {
            String substring = startsWith.substring(typedValue.length());
            completeOperation.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(substring));
            completeOperation.setOffset(completeOperation.getCursor());
            completeOperation.doAppendSeparator(false);
         }
         else
         {
            for (String choice : choices)
            {
               if (typedValue.isEmpty() || choice.startsWith(typedValue))
               {
                  completeOperation.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(choice));
               }
            }
            if (!completeOperation.getCompletionCandidates().isEmpty() && !typedValue.isEmpty())
            {
               completeOperation.setOffset(completeOperation.getCursor() - typedValue.length());
            }
         }
      }
      else if (choices.size() == 1)
      {
         String candidate = choices.get(0).substring(typedValue.length());
         completeOperation.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(candidate));
         completeOperation.setOffset(completeOperation.getCursor() - typedValue.length());
      }
   }
}