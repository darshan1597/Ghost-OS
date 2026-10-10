import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ghost_os/app/ghost_app.dart';
import 'package:ghost_os/features/assistant/installed_apps_sheet.dart';
import 'package:ghost_os/features/assistant/ghost_home.dart';

void main() {
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  const methods = MethodChannel('ghost/methods');
  const events = MethodChannel('ghost/events');
  final calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    binding.defaultBinaryMessenger.setMockMethodCallHandler(methods, (
      call,
    ) async {
      calls.add(call);
      return null;
    });
    binding.defaultBinaryMessenger.setMockMethodCallHandler(
      events,
      (_) async => null,
    );
  });
  tearDown(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(methods, null);
    binding.defaultBinaryMessenger.setMockMethodCallHandler(events, null);
  });

  Future<void> emit(WidgetTester tester, Map<String, Object?> state) async {
    await binding.defaultBinaryMessenger.handlePlatformMessage(
      'ghost/events',
      const StandardMethodCodec().encodeSuccessEnvelope(state),
      (_) {},
    );
    await tester.pump();
  }

  testWidgets('Listening requires explicit consent', (tester) async {
    await tester.pumpWidget(const GhostApp());
    await tester.ensureVisible(find.text('Enable Microphone'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enable Microphone'));
    await tester.pumpAndSettle();
    expect(calls, isEmpty);
    expect(find.text('Enable local listening?'), findsOneWidget);
    await tester.ensureVisible(find.text('Not now'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Not now'));
    await tester.pumpAndSettle();
    expect(calls, isEmpty);
    expect(find.text('Enable local listening?'), findsNothing);
  });

  testWidgets('Mic state follows native updates and disable stops listening', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    expect(find.text('Microphone off'), findsOneWidget);
    await emit(tester, {
      'listening': true,
      'status': 'Loading offline English model',
    });
    expect(find.text('Microphone starting'), findsOneWidget);
    await emit(tester, {
      'listening': true,
      'status': 'Listening for Hey Ghost',
    });
    expect(find.text('Microphone on'), findsOneWidget);
    await tester.ensureVisible(find.text('Disable microphone'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Disable microphone'));
    await tester.pumpAndSettle();
    expect(calls.single.method, 'stopListening');
    await emit(tester, {'listening': false});
    expect(find.text('Microphone off'), findsOneWidget);
  });

  testWidgets(
    'Narrow popup keeps consent reachable and enables only on approval',
    (tester) async {
      tester.view.physicalSize = const Size(320, 600);
      tester.view.devicePixelRatio = 1;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });
      await tester.pumpWidget(const GhostApp());
      await tester.ensureVisible(find.text('Enable Microphone'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Enable Microphone'));
      await tester.pumpAndSettle();
      expect(calls, isEmpty);
      expect(tester.takeException(), isNull);
      await tester.ensureVisible(find.text('Enable listening'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Enable listening'));
      await tester.pumpAndSettle();
      expect(calls.single.method, 'startListening');
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('Saved assistant name updates the home greeting', (tester) async {
    await tester.pumpWidget(const GhostApp());
    expect(find.widgetWithText(GradientText, 'Ghost'), findsOneWidget);
    await emit(tester, {'assistantName': 'Phantom'});
    expect(find.widgetWithText(GradientText, 'Phantom'), findsOneWidget);
    expect(find.text('Phantom'), findsOneWidget);
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Assistant name'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Assistant name'));
    await tester.pumpAndSettle();
    expect(find.text('Personalize your Ghost'), findsOneWidget);
    expect(find.byKey(const ValueKey('assistantNameField')), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'Specter');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(calls.single.method, 'setAssistantName');
    expect(calls.single.arguments, {'name': 'Specter'});
  });

  testWidgets('Text commands dispatch locally and preserve input', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await tester.scrollUntilVisible(
      find.byType(TextField),
      250,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byType(TextField),
      'message Harsh on WhatsApp with Hi!',
    );
    await tester.testTextInput.receiveAction(TextInputAction.send);
    await tester.pump();
    expect(calls.single.method, 'submit');
    expect(calls.single.arguments, {
      'text': 'message Harsh on WhatsApp with Hi!',
    });
  });

  testWidgets('Draft approval binds to the pending action identifier', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {
      'pending': {
        'id': 'test-action',
        'title': 'Prepare WhatsApp draft',
        'detail': 'To: Harsh\nMessage: hi\nReview and send manually.',
        'kind': 'message',
      },
    });
    expect(calls, isEmpty);
    await tester.tap(find.text('Open draft for review'));
    await tester.pump();
    expect(calls.single.method, 'approve');
    expect(calls.single.arguments, {'id': 'test-action'});
  });

  testWidgets('Voice mode can be disabled', (tester) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Off'), 250);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Off'));
    await tester.pump();
    expect(calls.single.method, 'setVoiceMode');
    expect(calls.single.arguments, {'mode': 'off'});
  });

  testWidgets('Glass settings keeps the hands-free launch switch working', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(
      find.byKey(const ValueKey('assistantBubbleEnabled')),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('assistantBubbleEnabled')));
    await tester.pump();
    expect(calls.single.method, 'setHandsFree');
    expect(calls.single.arguments, {'enabled': true});
  });

  testWidgets('Bubble size selection reaches the native settings', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Large (56 dp)').last);
    await tester.pumpAndSettle();
    expect(calls.single.method, 'setBubbleSize');
    expect(calls.single.arguments, {'size': 56});
  });

  testWidgets('Idle fading can be disabled independently', (tester) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    final fade = find.byKey(const ValueKey('bubbleIdleFade'));
    await tester.ensureVisible(fade);
    await tester.pumpAndSettle();
    await tester.tap(fade);
    await tester.pumpAndSettle();
    expect(calls.single.method, 'setBubbleFade');
    expect(calls.single.arguments, {'enabled': false});
  });

  testWidgets('Ghost Link connects only the selected paired device', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {
      'link': {
        'status': 'Not connected',
        'connected': false,
        'devices': [
          {'name': 'Second phone', 'address': 'AA:BB:CC:DD:EE:FF'},
        ],
      },
    });
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Connect'), 160);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Connect'));
    await tester.pump();
    expect(calls.single.method, 'linkConnect');
    expect(calls.single.arguments, {'address': 'AA:BB:CC:DD:EE:FF'});
  });

  testWidgets('App browser searches names and packages', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: InstalledAppsSheet(
            apps: const [
              {'name': 'Vivo Notes', 'package': 'com.vivo.notes'},
              {'name': 'YouTube', 'package': 'com.google.android.youtube'},
            ],
          ),
        ),
      ),
    );
    await tester.enterText(find.byType(TextField), 'notes');
    await tester.pump();
    expect(find.text('Vivo Notes'), findsOneWidget);
    expect(find.text('YouTube'), findsNothing);
    await tester.enterText(find.byType(TextField), 'notinstalled');
    await tester.pump();
    expect(find.text('No matching launchable apps.'), findsOneWidget);
  });

  testWidgets('Speak now bypasses wake phrase through native listener', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {'listening': true});
    await tester.ensureVisible(find.byTooltip('Speak now'));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Speak now'));
    await tester.pump();
    expect(calls.single.method, 'speakNow');
  });

  testWidgets('Duplicate contact selection passes only the chosen contact id', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {
      'contactChoices': [
        {'id': 'a', 'name': 'Harsh Kumar', 'phone': '+919876543210'},
        {'id': 'b', 'name': 'Harsh Gowda', 'phone': '+919876543211'},
      ],
    });
    await tester.tap(find.text('Harsh Gowda'));
    await tester.pump();
    expect(calls.single.method, 'chooseContact');
    expect(calls.single.arguments, {'id': 'b'});
  });

  testWidgets('Bundled voice can switch to the Android fallback', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    final selector = find.byKey(const ValueKey('preset-cinematic'));
    await tester.ensureVisible(selector);
    await tester.pumpAndSettle();
    await tester.tap(selector);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Android offline TTS').last);
    await tester.pumpAndSettle();
    expect(calls.single.method, 'setVoicePreset');
    expect(calls.single.arguments, {'preset': 'android'});
  });

  testWidgets('Voice character changes and aliases are removed', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    expect(find.text('Contact aliases'), findsNothing);
    await tester.scrollUntilVisible(find.text('Monster woman'), 240);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Monster woman'));
    await tester.pump();
    expect(calls.single.method, 'setVoiceCharacter');
    expect(calls.single.arguments, {'character': 'woman'});
  });

  testWidgets('Standard build hides automatic Send even with old preferences', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {
      'autoSendAvailable': false,
      'autoSend': true,
      'whatsappAccess': true,
    });
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('WhatsApp contacts'), 300);
    await tester.pumpAndSettle();
    expect(find.text('Send through WhatsApp'), findsNothing);
    expect(find.text('Open Accessibility settings'), findsNothing);
    expect(
      find.text(
        'I’ll open the right chat with your message ready. Check it and press Send in WhatsApp.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Automation build keeps its explicit Send option', (
    tester,
  ) async {
    await tester.pumpWidget(const GhostApp());
    await emit(tester, {'autoSendAvailable': true, 'autoSend': false});
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Send through WhatsApp'), 300);
    await tester.pumpAndSettle();
    await tester.tap(
      find.widgetWithText(SwitchListTile, 'Send through WhatsApp'),
    );
    await tester.pump();
    expect(calls.single.method, 'setAutoSend');
    expect(calls.single.arguments, {'enabled': true});
  });
}
