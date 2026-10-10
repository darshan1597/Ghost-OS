import 'package:flutter/material.dart';

import 'ghost_home.dart';

class ListeningConsentDialog extends StatelessWidget {
  const ListeningConsentDialog({super.key});

  @override
  Widget build(BuildContext context) => Dialog(
    backgroundColor: Colors.transparent,
    insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 440),
      child: SingleChildScrollView(
        child: CustomPaint(
          painter: _ConsentFrame(),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(18, 0, 18, 22),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Image.asset(
                  'assets/branding/ghost_hero.png',
                  height: 125,
                  fit: BoxFit.contain,
                  excludeFromSemantics: true,
                ),
                ShaderMask(
                  blendMode: BlendMode.srcIn,
                  shaderCallback: (bounds) => const LinearGradient(
                    colors: [
                      Colors.white,
                      Colors.white,
                      Color(0xFF00DFFF),
                      Color(0xFFA14EFF),
                    ],
                    stops: [0, .53, .65, 1],
                  ).createShader(bounds),
                  child: const Text(
                    'Enable local listening?',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 23,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                const Text(
                  'Ghost will use your microphone while listening is enabled, including with the screen off. Speech is processed on this phone. No audio is uploaded or recorded.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Color(0xFFC6D6F1),
                    fontSize: 13,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 5,
                    vertical: 13,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0x99081124),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFF224A7E)),
                  ),
                  child: const Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _Fact(Icons.shield_outlined, 'Processed\nlocally'),
                      _Fact(
                        Icons.cloud_off_outlined,
                        'No data\nuploaded',
                        purple: true,
                      ),
                      _Fact(Icons.lock_outline, 'Works with\nscreen off'),
                      _Fact(
                        Icons.battery_charging_full,
                        'Uses\nbattery',
                        purple: true,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Replies may use temporary private audio files, removed after playback. A notification lets you stop listening. Continuous listening consumes battery.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Color(0xFFAEBFE4),
                    fontSize: 12,
                    height: 1.5,
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Divider(color: Color(0xFF24456E)),
                ),
                const Row(
                  children: [
                    GhostWaveform(size: 30),
                    SizedBox(width: 15),
                    Expanded(
                      child: Text(
                        'Say “Hey Ghost”, wait for the greeting, then give your command.',
                        style: TextStyle(
                          color: Color(0xFFC6D6F1),
                          fontSize: 13,
                          height: 1.4,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 22),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => Navigator.pop(context, false),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFFB9CBEF),
                          side: const BorderSide(color: Color(0xFF31629A)),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: const Text('Not now'),
                      ),
                    ),
                    const SizedBox(width: 9),
                    Expanded(
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: neonGradient,
                          borderRadius: BorderRadius.circular(30),
                          boxShadow: const [
                            BoxShadow(color: Color(0x55423EFF), blurRadius: 17),
                          ],
                        ),
                        child: FilledButton(
                          onPressed: () => Navigator.pop(context, true),
                          style: FilledButton.styleFrom(
                            backgroundColor: Colors.transparent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 7,
                              vertical: 14,
                            ),
                          ),
                          child: const Text(
                            'Enable listening',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

class _Fact extends StatelessWidget {
  const _Fact(this.icon, this.text, {this.purple = false});
  final IconData icon;
  final String text;
  final bool purple;
  @override
  Widget build(BuildContext context) => Expanded(
    child: Column(
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: purple ? const Color(0xFF1D0D39) : const Color(0xFF051E39),
            border: Border.all(
              color: purple ? const Color(0xFF6836B7) : const Color(0xFF09649C),
            ),
          ),
          child: Icon(
            icon,
            size: 22,
            color: purple ? const Color(0xFFC48CFF) : const Color(0xFF00CEFF),
          ),
        ),
        const SizedBox(height: 7),
        Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: Color(0xFFDFE7F7),
            fontSize: 10,
            height: 1.4,
          ),
        ),
      ],
    ),
  );
}

class _ConsentFrame extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final path = Path()
      ..moveTo(0, 125)
      ..quadraticBezierTo(0, 72, 40, 72)
      ..cubicTo(w * .25, 72, w * .27, 109, w * .5, 109)
      ..cubicTo(w * .73, 109, w * .75, 72, w - 40, 72)
      ..quadraticBezierTo(w, 72, w, 125)
      ..lineTo(w, size.height - 26)
      ..quadraticBezierTo(w, size.height, w - 26, size.height)
      ..lineTo(26, size.height)
      ..quadraticBezierTo(0, size.height, 0, size.height - 26)
      ..close();
    final paint = Paint()
      ..shader = const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFF05162E), Color(0xFF030915), Color(0xFF111035)],
      ).createShader(Offset.zero & size);
    canvas.drawShadow(path, const Color(0xFF174EFF), 12, false);
    canvas.drawPath(path, paint);
    paint
      ..shader = neonGradient.createShader(Offset.zero & size)
      ..style = PaintingStyle.stroke
      ..strokeWidth = .8;
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
