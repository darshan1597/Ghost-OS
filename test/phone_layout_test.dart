import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ghost_os/app/ghost_app.dart';

void main() {
  testWidgets(
    'Phone layout renders assistant and voice settings without overflow',
    (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });
      const methods = MethodChannel('ghost/methods');
      const events = MethodChannel('ghost/events');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      messenger.setMockMethodCallHandler(methods, (_) async => null);
      messenger.setMockMethodCallHandler(events, (_) async => null);
      addTearDown(() {
        messenger.setMockMethodCallHandler(methods, null);
        messenger.setMockMethodCallHandler(events, null);
      });
      const fontDir = String.fromEnvironment('GHOST_FONT_DIR');
      if (fontDir.isNotEmpty) {
        await tester.runAsync(() async {
          for (final font in {
            'Roboto': 'roboto-regular.ttf',
            'MaterialIcons': 'materialicons-regular.otf',
          }.entries) {
            final loader = FontLoader(font.key);
            final bytes = await File('$fontDir/${font.value}').readAsBytes();
            loader.addFont(Future.value(ByteData.sublistView(bytes)));
            await loader.load();
          }
        });
      }
      final key = GlobalKey();
      await tester.pumpWidget(
        RepaintBoundary(key: key, child: const GhostApp()),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      Future<void> preview(String name) async {
        if (!const bool.fromEnvironment('GHOST_PREVIEW')) return;
        await tester.runAsync(() async {
          final image =
              await (key.currentContext!.findRenderObject()
                      as RenderRepaintBoundary)
                  .toImage(pixelRatio: 1);
          final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
          Directory('build/previews').createSync(recursive: true);
          File('build/previews/$name.png')
              .writeAsBytesSync(bytes!.buffer.asUint8List());
          image.dispose();
        });
      }

      await preview('assistant');
      await tester.tap(find.text('Settings'));
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Monster woman'), 200);
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await preview('voice-settings');
    },
  );
}
