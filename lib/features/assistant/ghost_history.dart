import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'ghost_home.dart';

class GhostHistory extends StatelessWidget {
  const GhostHistory({super.key, required this.history, required this.onClear});
  final List<dynamic> history;
  final VoidCallback onClear;
  String day(DateTime date) {
    final now = DateTime.now();
    final difference = DateTime(
      now.year,
      now.month,
      now.day,
    ).difference(DateTime(date.year, date.month, date.day)).inDays;
    if (difference == 0) return 'Today';
    if (difference == 1) return 'Yesterday';
    return '${date.day}/${date.month}/${date.year}';
  }

  @override
  Widget build(BuildContext context) {
    final groups = <String, List<Map>>{};
    for (final entry in history) {
      final item = entry as Map;
      final date = DateTime.fromMillisecondsSinceEpoch(
        (item['time'] as num? ?? DateTime.now().millisecondsSinceEpoch).toInt(),
      );
      groups.putIfAbsent(day(date), () => []).add(item);
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 15),
        const Row(
          children: [
            Text(
              'Ghost',
              style: TextStyle(
                fontSize: 30,
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
            GradientText(
              'OS',
              style: TextStyle(fontSize: 30, fontWeight: FontWeight.w700),
            ),
          ],
        ),
        const SizedBox(height: 5),
        const Text(
          'History',
          style: TextStyle(fontSize: 15, color: Color(0xFFB3C6EC)),
        ),
        const SizedBox(height: 30),
        Row(
          children: [
            const Expanded(
              child: Text(
                'Activity',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
            OutlinedButton.icon(
              onPressed: history.isEmpty ? null : onClear,
              icon: const Icon(Icons.delete_outline, size: 18),
              label: const Text('Clear all'),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFFFF3854),
                disabledForegroundColor: const Color(0x99FF3854),
                side: const BorderSide(color: Color(0xFFFF3854)),
                shape: const StadiumBorder(),
              ),
            ),
          ],
        ),
        const SizedBox(height: 5),
        const Text(
          'Your conversations and actions with Ghost.',
          style: TextStyle(fontSize: 13, color: Color(0xFFB3C6EC)),
        ),
        const SizedBox(height: 22),
        if (history.isEmpty) ...[
          _glass(
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 14, vertical: 7),
              child: Text(
                'Today',
                style: TextStyle(fontSize: 14, color: Color(0xFFB3C6EC)),
              ),
            ),
          ),
          const SizedBox(height: 8),
          _entry(context, {
            'command': 'No activity yet',
            'outcome':
                'Your command results will appear here. Nothing has run yet.',
          }, empty: true),
        ],
        for (final group in groups.entries) ...[
          _glass(
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
              child: Text(
                group.key,
                style: const TextStyle(fontSize: 14, color: Color(0xFFB3C6EC)),
              ),
            ),
          ),
          const SizedBox(height: 8),
          for (final item in group.value) ...[
            _entry(context, item),
            const SizedBox(height: 10),
          ],
          const SizedBox(height: 4),
        ],
      ],
    );
  }

  Widget _entry(BuildContext context, Map item, {bool empty = false}) {
    final result = item['outcome'] as String? ?? '';
    final time = DateTime.fromMillisecondsSinceEpoch(
      (item['time'] as num? ?? DateTime.now().millisecondsSinceEpoch).toInt(),
    );
    // Dispatch/request logs do not prove delivery or that another app saved an alarm.
    final waiting = RegExp(
      'waiting|awaiting|which|several contacts|what time',
      caseSensitive: false,
    ).hasMatch(result);
    final cancelled = RegExp(
      'cancelled|expired',
      caseSensitive: false,
    ).hasMatch(result);
    final label = empty
        ? 'Ready for your first command'
        : cancelled
        ? 'Cancelled / expired'
        : waiting
        ? 'Needs attention'
        : 'Response recorded';
    final color = empty
        ? const Color(0xFFB3C6EC)
        : waiting
        ? const Color(0xFFFFC460)
        : cancelled
        ? const Color(0xFFFF6581)
        : const Color(0xFF00DDB8);
    return _glass(
      Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 39,
              height: 39,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: neonGradient,
              ),
              padding: const EdgeInsets.all(1.3),
              child: Container(
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Color(0xFF0C1230),
                ),
                child: const Center(child: GhostWaveform(size: 23)),
              ),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          item['command'] as String? ?? 'Ghost action',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      if (!empty) ...[
                        const SizedBox(width: 8),
                        Text(
                          TimeOfDay.fromDateTime(time).format(context),
                          style: const TextStyle(
                            fontSize: 10,
                            color: Color(0xFFAFBEE1),
                          ),
                        ),
                      ],
                      if (!empty)
                        SizedBox(
                          width: 24,
                          height: 24,
                          child: PopupMenuButton<String>(
                            padding: EdgeInsets.zero,
                            tooltip: 'Activity options',
                            icon: const Icon(
                              Icons.more_vert,
                              size: 18,
                              color: Color(0xFFAFBEE1),
                            ),
                            itemBuilder: (_) => const [
                              PopupMenuItem(
                                value: 'copy',
                                child: Text('Copy details'),
                              ),
                            ],
                            onSelected: (_) => Clipboard.setData(
                              ClipboardData(
                                text: '${item['command']}\n$result',
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    result,
                    style: const TextStyle(
                      fontSize: 12,
                      height: 1.4,
                      color: Color(0xFFB3C6EC),
                    ),
                  ),
                  const SizedBox(height: 11),
                  Row(
                    children: [
                      Icon(
                        empty
                            ? Icons.radio_button_unchecked
                            : waiting
                            ? Icons.info
                            : cancelled
                            ? Icons.cancel
                            : Icons.check_circle,
                        size: 15,
                        color: color,
                      ),
                      const SizedBox(width: 5),
                      Text(label, style: TextStyle(fontSize: 11, color: color)),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
      bright: true,
    );
  }

  Widget _glass(Widget child, {bool bright = false}) => Container(
    decoration: BoxDecoration(
      gradient: const LinearGradient(
        colors: [Color(0xB3071D35), Color(0xBD060E22)],
      ),
      borderRadius: BorderRadius.circular(18),
      border: Border.all(
        color: bright ? const Color(0xFF0079D5) : const Color(0xFF173E6C),
      ),
    ),
    child: child,
  );
}
