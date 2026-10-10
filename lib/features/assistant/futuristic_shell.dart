import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'ghost_avatar.dart';

const ghostCyan = Color(0xFF48DEFF);
const ghostPurple = Color(0xFFA366FF);

class GhostBackdrop extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    paint.shader = const RadialGradient(
      colors: [Color(0xFF0B1230), Color(0xFF02040A)],
      radius: .85,
      center: Alignment(.5, -.7),
    ).createShader(Offset.zero & size);
    canvas.drawRect(Offset.zero & size, paint);
    paint.shader = null;
    for (var i = 0; i < 5; i++) {
      final path = Path()
        ..moveTo(-40, size.height * (.12 + i * .055))
        ..cubicTo(
          size.width * .25,
          size.height * .14,
          size.width * .12,
          size.height * .62,
          size.width * .55,
          size.height * .57,
        )
        ..cubicTo(
          size.width * .8,
          size.height * .65,
          size.width * .94,
          size.height * .33,
          size.width + 50,
          size.height * (.35 + i * .05),
        );
      paint.shader = const LinearGradient(
        colors: [Color(0x55003697), Color(0x14011C5C), Color(0x44250074)],
      ).createShader(Offset.zero & size);
      paint.style = PaintingStyle.stroke;
      paint.strokeWidth = 20 - i * 3;
      paint.maskFilter = const MaskFilter.blur(BlurStyle.normal, 12);
      canvas.drawPath(path, paint);
    }
    paint
      ..shader = null
      ..style = PaintingStyle.fill
      ..maskFilter = null;
    for (var i = 0; i < 18; i++) {
      final point = Offset(
        (i * 127.3 + 44) % size.width,
        size.height * .12 + (i * 39.7) % (size.height * .45),
      );
      paint.color = (i.isEven ? ghostCyan : ghostPurple).withValues(alpha: .35);
      paint.maskFilter = const MaskFilter.blur(BlurStyle.normal, 3);
      canvas.drawCircle(point, i % 3 == 0 ? 2.0 : 1.0, paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class GhostCore extends StatefulWidget {
  const GhostCore({super.key, required this.active});
  final bool active;
  @override
  State<GhostCore> createState() => _GhostCoreState();
}

class _GhostCoreState extends State<GhostCore>
    with SingleTickerProviderStateMixin {
  late final AnimationController _orbit = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 12),
  );
  @override
  void initState() {
    super.initState();
    if (widget.active) _orbit.repeat();
  }

  @override
  void didUpdateWidget(GhostCore oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active) {
      if (!_orbit.isAnimating) _orbit.repeat();
    } else {
      _orbit.stop();
    }
  }

  @override
  void dispose() {
    _orbit.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Semantics(
    label: widget.active ? 'Ghost listening core' : 'Ghost resting core',
    child: AnimatedBuilder(
      animation: _orbit,
      builder: (context, child) => CustomPaint(
        painter: _OrbitPainter(_orbit.value, widget.active),
        child: child,
      ),
      child: SizedBox(
        width: 230,
        height: 230,
        child: Center(
          child: Container(
            width: 142,
            height: 142,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: const Color(0xFF0C1529),
              boxShadow: [
                BoxShadow(
                  color: ghostPurple.withValues(alpha: .2),
                  blurRadius: 40,
                ),
              ],
            ),
            child: const ClipOval(child: GhostAvatar()),
          ),
        ),
      ),
    ),
  );
}

class _OrbitPainter extends CustomPainter {
  _OrbitPainter(this.phase, this.active);
  final double phase;
  final bool active;
  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1;
    for (final radius in [87.0, 103.0, 113.0]) {
      paint.color = ghostCyan.withValues(alpha: .16);
      canvas.drawCircle(center, radius, paint);
    }
    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.rotate(phase * math.pi * 2);
    paint.shader = const SweepGradient(
      colors: [ghostCyan, ghostPurple, Color(0x0048DEFF)],
    ).createShader(const Rect.fromLTWH(-104, -104, 208, 208));
    paint.strokeWidth = active ? 3 : 2;
    canvas.drawArc(
      const Rect.fromLTWH(-103, -103, 206, 206),
      0,
      4.5,
      false,
      paint,
    );
    paint.shader = null;
    paint.color = ghostCyan;
    for (var i = 0; i < 48; i++) {
      final a = i * math.pi / 24;
      canvas.drawLine(
        Offset(math.cos(a) * 110, math.sin(a) * 110),
        Offset(
          math.cos(a) * (i % 4 == 0 ? 117 : 113),
          math.sin(a) * (i % 4 == 0 ? 117 : 113),
        ),
        paint,
      );
    }
    paint.style = PaintingStyle.fill;
    canvas.drawCircle(const Offset(103, 0), 4, paint);
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _OrbitPainter oldDelegate) =>
      phase != oldDelegate.phase || active != oldDelegate.active;
}
