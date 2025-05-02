from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import yt_dlp
import os
import tempfile
import json
from datetime import datetime
import logging

app = Flask(__name__)
CORS(app)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configure yt-dlp options
YDL_OPTS = {
    'quiet': True,
    'no_warnings': True,
    'extract_flat': False,
}

def format_size(bytes):
    """Convert bytes to human readable string"""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes < 1024.0:
            return f"{bytes:3.1f} {unit}"
        bytes /= 1024.0
    return f"{bytes:.1f} TB"

def clean_formats(formats):
    """Clean and filter formats for the response"""
    cleaned = []
    seen_qualities = set()
    
    for f in formats:
        # Skip formats without url or format_id
        if not f.get('url') or not f.get('format_id'):
            continue
            
        # Basic format info
        format_info = {
            'formatId': f.get('format_id'),
            'ext': f.get('ext', ''),
            'filesize': f.get('filesize', f.get('filesize_approx')),
            'url': f.get('url'),
        }
        
        # Handle video formats
        if f.get('vcodec') != 'none':
            height = f.get('height')
            if height:
                quality = f"{height}p"
                # Skip duplicate qualities
                if quality in seen_qualities:
                    continue
                seen_qualities.add(quality)
                format_info.update({
                    'resolution': quality,
                    'formatNote': f.get('format_note'),
                    'vcodec': f.get('vcodec'),
                    'acodec': f.get('acodec'),
                    'isVideoOnly': f.get('acodec') == 'none'
                })
                cleaned.append(format_info)
                
        # Handle audio formats
        elif f.get('acodec') != 'none':
            format_info.update({
                'formatNote': f.get('format_note'),
                'acodec': f.get('acodec'),
                'isAudioOnly': True
            })
            cleaned.append(format_info)
    
    return cleaned

@app.route('/metadata', methods=['POST'])
def get_metadata():
    try:
        data = request.get_json()
        url = data.get('url')
        
        if not url:
            return jsonify({
                'success': False,
                'error': 'No URL provided'
            }), 400

        logger.info(f"Fetching metadata for URL: {url}")
        
        with yt_dlp.YoutubeDL(YDL_OPTS) as ydl:
            info = ydl.extract_info(url, download=False)
            
            # Clean and prepare the response
            metadata = {
                'id': info.get('id'),
                'title': info.get('title'),
                'thumbnailUrl': info.get('thumbnail'),
                'duration': str(info.get('duration')),
                'uploader': info.get('uploader'),
                'uploadDate': info.get('upload_date'),
                'viewCount': info.get('view_count'),
                'description': info.get('description'),
                'formats': clean_formats(info.get('formats', []))
            }
            
            return jsonify({
                'success': True,
                'data': metadata
            })

    except Exception as e:
        logger.error(f"Error processing URL: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/download', methods=['POST'])
def download_video():
    try:
        data = request.get_json()
        url = data.get('url')
        format_id = data.get('formatId')
        
        if not url or not format_id:
            return jsonify({
                'success': False,
                'error': 'Missing URL or format ID'
            }), 400

        logger.info(f"Starting download for URL: {url}, format: {format_id}")
        
        # Create temporary directory for download
        with tempfile.TemporaryDirectory() as temp_dir:
            output_template = os.path.join(temp_dir, '%(title)s.%(ext)s')
            
            ydl_opts = {
                'format': format_id,
                'outtmpl': output_template,
                'quiet': True,
                'no_warnings': True
            }
            
            # Download the video
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                downloaded_file = ydl.prepare_filename(info)
                
                if not os.path.exists(downloaded_file):
                    raise Exception("Download failed - file not found")
                
                # Send the file
                return send_file(
                    downloaded_file,
                    as_attachment=True,
                    download_name=os.path.basename(downloaded_file)
                )

    except Exception as e:
        logger.error(f"Download error: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
