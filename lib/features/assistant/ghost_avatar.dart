import 'package:flutter/material.dart';

/// The original logo is loaded when available; a code-drawn ghost is the fallback.
class GhostAvatar extends StatelessWidget {
  const GhostAvatar({super.key});
  @override
  Widget build(BuildContext context) => Image.asset(
    'assets/branding/ghost_logo.png',
    fit: BoxFit.cover,
    errorBuilder: (_, _, _) => CustomPaint(painter: _GhostPainter()),
  );
}

class _GhostPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.scale(size.width / 140, size.height / 140);
    final rect = const Rect.fromLTWH(24, 16, 94, 110);
    final ghost = Path()
      ..moveTo(37, 100)
      ..cubicTo(36, 68, 40, 27, 72, 24)
      ..cubicTo(103, 21, 111, 52, 108, 78)
      ..cubicTo(108, 94, 116, 101, 120, 105)
      ..quadraticBezierTo(106, 111, 97, 104)
      ..quadraticBezierTo(84, 129, 69, 117)
      ..quadraticBezierTo(57, 108, 44, 118)
      ..quadraticBezierTo(46, 106, 37, 100)
      ..close();
    final fill = Paint()
      ..shader = const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFF70F7FF), Color(0xFF2657EB), Color(0xFFAC58FF)],
      ).createShader(rect);
    canvas.drawPath(ghost, fill);
    final outline = Paint()
      ..color = const Color(0xFFB9FBFF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawPath(ghost, outline);
    canvas.drawOval(
      const Rect.fromLTWH(63, 53, 12, 22),
      Paint()..color = Colors.white,
    );
    canvas.drawOval(
      const Rect.fromLTWH(88, 57, 11, 21),
      Paint()..color = Colors.white,
    );
    final orbit = Paint()
      ..shader = const LinearGradient(
        colors: [Color(0xFF30E6FF), Color(0xFFAA4DFF)],
      ).createShader(rect)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 5;
    canvas.save();
    canvas.translate(70, 87);
    canvas.rotate(.3);
    canvas.drawArc(
      const Rect.fromLTWH(-58, -17, 116, 34),
      0,
      3.6,
      false,
      orbit,
    );
    canvas.restore();
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
