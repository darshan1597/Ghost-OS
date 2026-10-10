import 'package:flutter/material.dart';

import 'ghost_home.dart';

class PersonalizationDialog extends StatefulWidget {
  const PersonalizationDialog({required this.initialName, super.key});

  final String initialName;

  @override
  State<PersonalizationDialog> createState() => _PersonalizationDialogState();
}

class _PersonalizationDialogState extends State<PersonalizationDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _save() => Navigator.pop(context, _controller.text);

  @override
  Widget build(BuildContext context) => Dialog(
    backgroundColor: Colors.transparent,
    insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 440),
      child: SingleChildScrollView(
        child: CustomPaint(
          painter: _PersonalizationFrame(),
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
                    stops: [0, .45, .67, 1],
                  ).createShader(bounds),
                  child: const Text(
                    'Personalize your Ghost',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 23,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Choose the name shown throughout Ghost OS. You can change it again whenever you want.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Color(0xFFC6D6F1),
                    fontSize: 13,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 18),
                Container(
                  padding: const EdgeInsets.fromLTRB(14, 12, 14, 8),
                  decoration: BoxDecoration(
                    color: const Color(0x99081124),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFF224A7E)),
                    boxShadow: const [
                      BoxShadow(color: Color(0x332345FF), blurRadius: 18),
                    ],
                  ),
                  child: TextField(
                    key: const ValueKey('assistantNameField'),
                    controller: _controller,
                    autofocus: true,
                    maxLength: 24,
                    textCapitalization: TextCapitalization.words,
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _save(),
                    decoration: const InputDecoration(
                      labelText: 'Assistant name',
                      hintText: 'Ghost',
                      prefixIcon: Icon(
                        Icons.auto_awesome,
                        color: Color(0xFF30DFFF),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.graphic_eq, size: 28, color: Color(0xFFB488FF)),
                    SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'This changes the app name, spoken replies, and offline wake phrase. Short English names work best. “Hey Ghost” remains a fallback.',
                        style: TextStyle(
                          color: Color(0xFFAEBFE4),
                          fontSize: 12,
                          height: 1.45,
                        ),
                      ),
                    ),
                  ],
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 14),
                  child: Divider(color: Color(0xFF24456E)),
                ),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => Navigator.pop(context),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFFB9CBEF),
                          side: const BorderSide(color: Color(0xFF31629A)),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: const Text('Cancel'),
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
                          onPressed: _save,
                          style: FilledButton.styleFrom(
                            backgroundColor: Colors.transparent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                          ),
                          child: const Text('Save'),
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

class _PersonalizationFrame extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final width = size.width;
    final path = Path()
      ..moveTo(0, 125)
      ..quadraticBezierTo(0, 72, 40, 72)
      ..cubicTo(width * .25, 72, width * .27, 109, width * .5, 109)
      ..cubicTo(width * .73, 109, width * .75, 72, width - 40, 72)
      ..quadraticBezierTo(width, 72, width, 125)
      ..lineTo(width, size.height - 26)
      ..quadraticBezierTo(width, size.height, width - 26, size.height)
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
