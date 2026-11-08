FROM python:3.12-slim

RUN pip install --no-cache-dir "piper-tts[http]==1.8.0"

COPY start.sh /usr/local/bin/start-piper
RUN chmod +x /usr/local/bin/start-piper

EXPOSE 5000
ENTRYPOINT ["/usr/local/bin/start-piper"]
