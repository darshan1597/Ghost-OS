import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ghost_os/features/assistant/ghost_history.dart';

void main() {
  testWidgets(
    'Empty History keeps the date divider and activity card styling',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: GhostHistory(history: const [], onClear: () {}),
            ),
          ),
        ),
      );
      expect(find.text('Today'), findsOneWidget);
      expect(find.text('No activity yet'), findsOneWidget);
      expect(find.text('Ready for your first command'), findsOneWidget);
      expect(find.text('Response recorded'), findsNothing);
      expect(find.byTooltip('Activity options'), findsNothing);
      expect(
        tester
            .widget<OutlinedButton>(
              find.widgetWithText(OutlinedButton, 'Clear all'),
            )
            .onPressed,
        isNull,
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'History groups real outcomes and clears without claiming delivery',
    (tester) async {
      var cleared = false;
      final now = DateTime.now();
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: GhostHistory(
                history: [
                  {
                    'command': 'Set an alarm',
                    'outcome': 'Asked your clock to set an alarm.',
                    'time': now.millisecondsSinceEpoch,
                  },
                  {
                    'command': 'Open Gmail',
                    'outcome': 'Opening Gmail, boss.',
                    'time': DateTime(
                      now.year,
                      now.month,
                      now.day - 1,
                      12,
                    ).millisecondsSinceEpoch,
                  },
                ],
                onClear: () => cleared = true,
              ),
            ),
          ),
        ),
      );
      expect(find.text('Today'), findsOneWidget);
      expect(find.text('Yesterday'), findsOneWidget);
      expect(find.text('Opening Gmail, boss.'), findsOneWidget);
      expect(find.text('Completed'), findsNothing);
      await tester.tap(find.text('Clear all'));
      expect(cleared, isTrue);
      expect(tester.takeException(), isNull);
    },
  );
}
